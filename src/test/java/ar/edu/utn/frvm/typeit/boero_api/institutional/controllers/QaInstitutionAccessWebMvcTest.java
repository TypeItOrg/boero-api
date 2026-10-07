package ar.edu.utn.frvm.typeit.boero_api.institutional.controllers;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ar.edu.utn.frvm.typeit.boero_api.auth.services.*;
import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.*;
import ar.edu.utn.frvm.typeit.boero_api.authorization.security.*;
import ar.edu.utn.frvm.typeit.boero_api.authorization.services.*;
import ar.edu.utn.frvm.typeit.boero_api.common.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frvm.typeit.boero_api.config.WebConfig;
import ar.edu.utn.frvm.typeit.boero_api.institutional.exceptions.*;
import ar.edu.utn.frvm.typeit.boero_api.institutional.payloads.*;
import ar.edu.utn.frvm.typeit.boero_api.institutional.services.*;
import ar.edu.utn.frvm.typeit.boero_api.support.AuthTestData;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.PathMatcher;

@WebMvcTest({
  InstitutionController.class,
  PlatformInstitutionController.class,
  InstitutionalInstitutionController.class
})
@Import({
  PermissionAuthorizationAspect.class,
  RoleAuthorizationAspect.class,
  InstitutionAccessAspect.class,
  InstitutionalCallerGuard.class,
  GlobalExceptionHandler.class,
  WebConfig.class
})
@EnableAspectJAutoProxy
@AutoConfigureMockMvc(addFilters = false)
class QaInstitutionAccessWebMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean PathMatcher matcher;
  @MockitoBean AuthenticationEntryPoint entryPoint;
  @MockitoBean JwtService jwt;
  @MockitoBean TokenBlacklistService blacklist;
  @MockitoBean IsSessionActiveUseCase sessions;
  @MockitoBean IsPlatformSessionActiveUseCase platformSessions;
  @MockitoBean AuthorizationService authorization;
  @MockitoBean ListInstitutionsUseCase list;
  @MockitoBean GetInstitutionUseCase get;
  @MockitoBean ListInstitutionsAdminUseCase adminList;
  @MockitoBean GetInstitutionAdminUseCase adminGet;
  @MockitoBean CreateInstitutionUseCase create;
  @MockitoBean UpdateInstitutionUseCase update;
  @MockitoBean UpdateInstitutionStatusUseCase statusUpdate;
  @MockitoBean UpdateInstitutionalInstitutionUseCase tenantUpdate;
  @MockitoBean UpdateInstitutionPublicAccessUseCase publicAccess;
  @MockitoBean ResolveInstitutionPublicAccessUseCase resolve;
  @MockitoBean InstitutionLogoUseCase logos;
  final UUID institutionId = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  TestingAuthenticationToken platform() {
    return new TestingAuthenticationToken(AuthTestData.platformPrincipal(UUID.randomUUID()), "");
  }

  TestingAuthenticationToken tenant() {
    return new TestingAuthenticationToken(
        AuthTestData.institutionalPrincipal(UUID.randomUUID(), institutionId), "");
  }

  @Test
  void I02_minimalResolution_httpOnlyPublicFourFields() throws Exception {
    when(resolve.execute("cboero"))
        .thenReturn(
            new InstitutionPublicAccessResponse(institutionId, "Conservatorio QA", "cboero", null));
    final var response =
        mvc.perform(get("/api/v1/institutions/by-subdomain/cboero"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicSubdomain").value("cboero"))
            .andExpect(jsonPath("$.logoUrl").isEmpty())
            .andReturn()
            .getResponse();
    final var body =
        new tools.jackson.databind.json.JsonMapper().readTree(response.getContentAsString());
    assertThat(body.properties()).hasSize(4);
    assertThat(body.has("slug")).isFalse();
    assertThat(body.has("email")).isFalse();
  }

  @Test
  void I02_unknownInactive_http404AndUnavailable503() throws Exception {
    when(resolve.execute("absent")).thenThrow(new InstitutionNotFoundException());
    when(resolve.execute("offline")).thenThrow(new InstitutionPublicAccessUnavailableException());
    mvc.perform(get("/api/v1/institutions/by-subdomain/absent")).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/institutions/by-subdomain/offline"))
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void I01_domainInvariant_publicAccessPlatformOnly() throws Exception {
    when(authorization.hasPlatformRole(any(), eq(PlatformRoleCode.PLATFORM_ADMIN)))
        .thenReturn(true);
    mvc.perform(
            patch("/api/v1/admin/institutions/{id}/public-access", institutionId)
                .principal(platform())
                .contentType("application/json")
                .content("{\"publicSubdomain\":\"cboero\"}"))
        .andExpect(status().isOk());
    verify(publicAccess).execute(institutionId, "cboero");
    when(authorization.hasPlatformRole(any(), eq(PlatformRoleCode.PLATFORM_ADMIN)))
        .thenReturn(false);
    mvc.perform(
            patch("/api/v1/admin/institutions/{id}/public-access", institutionId)
                .principal(tenant())
                .contentType("application/json")
                .content("{\"publicSubdomain\":null}"))
        .andExpect(status().isForbidden());
    verify(publicAccess, never()).execute(institutionId, null);
  }

  @Test
  void I01_domainInvariant_publicAccessOmissionCannotClear() throws Exception {
    when(authorization.hasPlatformRole(any(), eq(PlatformRoleCode.PLATFORM_ADMIN)))
        .thenReturn(true);
    mvc.perform(
            patch("/api/v1/admin/institutions/{id}/public-access", institutionId)
                .principal(platform())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(publicAccess);
    mvc.perform(
            patch("/api/v1/admin/institutions/{id}/public-access", institutionId)
                .principal(platform())
                .contentType("application/json")
                .content("{\"publicSubdomain\":null}"))
        .andExpect(status().isOk());
    verify(publicAccess).execute(institutionId, null);
  }

  @Test
  void L01_platformLogoLifecycle_httpPutDelete() throws Exception {
    when(authorization.hasPlatformRole(any(), eq(PlatformRoleCode.PLATFORM_ADMIN)))
        .thenReturn(true);
    final var file = new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1});
    mvc.perform(
            multipart(HttpMethod.PUT, "/api/v1/admin/institutions/{id}/logo", institutionId)
                .file(file)
                .principal(platform()))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/v1/admin/institutions/{id}/logo", institutionId).principal(platform()))
        .andExpect(status().isNoContent());
    verify(logos).replace(eq(institutionId), any());
    verify(logos).delete(institutionId);
  }

  @Test
  void L02_institutionLogoLifecycle_httpPutDeleteOwnTenantPermission() throws Exception {
    when(authorization.hasPermission(any(), eq(PermissionCode.INSTITUTION_UPDATE)))
        .thenReturn(true);
    final var file = new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1});
    mvc.perform(
            multipart(HttpMethod.PUT, "/api/v1/institutions/{id}/logo", institutionId)
                .file(file)
                .principal(tenant()))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/v1/institutions/{id}/logo", institutionId).principal(tenant()))
        .andExpect(status().isNoContent());
    verify(logos).replace(eq(institutionId), any());
    verify(logos).delete(institutionId);
  }

  @Test
  void L02_logoAuthorization_missingPermissionOrCrossTenantOrPlatformRoleCannotWrite()
      throws Exception {
    mvc.perform(delete("/api/v1/institutions/{id}/logo", institutionId).principal(tenant()))
        .andExpect(status().isForbidden());
    when(authorization.hasPermission(any(), eq(PermissionCode.INSTITUTION_UPDATE)))
        .thenReturn(true);
    mvc.perform(delete("/api/v1/institutions/{id}/logo", UUID.randomUUID()).principal(tenant()))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/v1/admin/institutions/{id}/logo", institutionId).principal(tenant()))
        .andExpect(status().isForbidden());
    verifyNoInteractions(logos);
  }

  @Test
  void L03_logoCacheFallback_currentBytesRevalidateAndNoLogo404() throws Exception {
    when(logos.get(institutionId))
        .thenReturn(
            new InstitutionLogoUseCase.Logo(
                new ByteArrayResource(new byte[] {1, 2, 3}), "image/png", 3, "opaque-version"));
    mvc.perform(get("/api/v1/institutions/{id}/logo?v=old-version", institutionId))
        .andExpect(status().isOk())
        .andExpect(content().bytes(new byte[] {1, 2, 3}))
        .andExpect(content().contentType("image/png"))
        .andExpect(header().string("Cache-Control", "no-cache"))
        .andExpect(header().string("ETag", "\"opaque-version\""))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    when(logos.get(institutionId)).thenThrow(new InstitutionLogoNotFoundException());
    mvc.perform(get("/api/v1/institutions/{id}/logo", institutionId))
        .andExpect(status().isNotFound());
  }
}
