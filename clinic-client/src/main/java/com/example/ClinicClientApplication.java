package com.example;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.Resources;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@EnableBinding(ClientChannels.class)
@EnableCircuitBreaker
@EnableFeignClients
@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication
public class ClinicClientApplication {

    @Bean
    MessageChannel clientChannel(ClientChannels clientChannels) {
        return clientChannels.output();
    }

	public static void main(String[] args) {
		SpringApplication.run(ClinicClientApplication.class, args);
	}
}

@RestController
class ClientController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private MessageChannel clientChannel;

    @RequestMapping(value = "names", method = RequestMethod.GET)
    public List<String> names() {
        return clientService.getVar();
    }

    @RequestMapping(value = "write", method = RequestMethod.GET)
    public Boolean clientChannel(String name) {
        return clientChannel.send(MessageBuilder.withPayload(name).build());
    }
}

@Service
class ClientService {
    @Autowired
    private ClinicClient clinicClient;

    public List<String> fallback() {
        return Collections.emptyList();
    }

    @HystrixCommand(fallbackMethod = "fallback")
    public List<String> getVar() {
        return clinicClient.read().getContent()
                .stream()
                .map(Clinic::getName)
                .collect(Collectors.toList());
    }

}

@FeignClient("clinic-service")
interface ClinicClient{

    @RequestMapping(value = "clinics", method = RequestMethod.GET)
    Resources<Clinic> read();
}

interface ClientChannels{

    @Output
    MessageChannel output();
}

@Data
@NoArgsConstructor
class Clinic {
    private Long id;
    private String name;
}