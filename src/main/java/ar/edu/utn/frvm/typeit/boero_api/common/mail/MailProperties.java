package ar.edu.utn.frvm.typeit.boero_api.common.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String from) {}
