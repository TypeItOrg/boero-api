package ar.edu.utn.frvm.typeit.boero_api.common.mail;

public final class MailSendingException extends RuntimeException {

  public MailSendingException(final Throwable cause) {
    super(MailMessages.SEND_FAILED, cause);
  }
}
