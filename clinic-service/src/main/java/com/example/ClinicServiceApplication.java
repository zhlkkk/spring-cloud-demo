package com.example;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.stream.Stream;

@EnableBinding(ClientChannels.class)
@SpringBootApplication
@EnableDiscoveryClient
public class ClinicServiceApplication {

    @Bean
    CommandLineRunner runner(ClinicRepo clinicRepo) {
        return args -> {
            Stream.of("sh", "beijing").forEach(c -> clinicRepo.save(new Clinic(c)));
            clinicRepo.findAll();
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(ClinicServiceApplication.class, args);
    }
}

@RefreshScope
@RestController
class ClinicController{

    @Autowired
    @Value("${message}")
    private String message;

    @RequestMapping(value = "message", method = RequestMethod.GET)
    public String message() {
        return message;
    }
}

@MessageEndpoint
class MessageProcess{

    @Autowired
    ClinicRepo clinicRepo;

    @ServiceActivator(inputChannel = "input")
    public void write(Message<String> name) {
        clinicRepo.save(new Clinic(name.getPayload()));
    }
}

@RepositoryRestResource
interface ClinicRepo extends JpaRepository<Clinic, Long> {
}

interface ClientChannels{
    @Input
    SubscribableChannel input();
}

@Entity
@Data
@NoArgsConstructor
class Clinic {
    @Id @GeneratedValue
    private Long id;
    private String name;

    public Clinic(String name) {
        this.name = name;
    }
}