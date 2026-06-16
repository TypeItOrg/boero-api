package ar.edu.utn.frvm.typeit.boero_api.authorization;

import ar.edu.utn.frvm.typeit.boero_api.authorization.enums.PlatformRoleCode;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPlatformRole {
  PlatformRoleCode value();
}
