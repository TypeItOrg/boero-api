package ar.edu.utn.frvm.typeit.boero_api.institutional.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InstitutionalDomainTest {

  @Test
  @DisplayName("Should report only real institution status transitions")
  void institutionReportsStatusTransitions() {
    final Institution institution = Institution.builder().active(true).build();

    assertThat(institution.updateStatus(false)).isTrue();
    assertThat(institution.updateStatus(false)).isFalse();
    assertThat(institution.isActive()).isFalse();
  }

  @Test
  @DisplayName("Should reject an address from another institution")
  void personRejectsAddressFromAnotherInstitution() {
    final Institution institution = Institution.builder().id(UUID.randomUUID()).build();
    final Institution otherInstitution = Institution.builder().id(UUID.randomUUID()).build();
    final Person person = Person.builder().institution(institution).build();
    final Address address = Address.builder().institution(otherInstitution).build();

    assertThatThrownBy(() -> person.changeAddress(address))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should soft delete a person only once")
  void personDeletesOnlyOnce() {
    final Person person = Person.builder().build();

    assertThat(person.delete()).isTrue();
    assertThat(person.delete()).isFalse();
    assertThat(person.isDeleted()).isTrue();
  }
}
