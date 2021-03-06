package de.ksbrwsk.people;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;

import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Component
@Log4j2
@RequiredArgsConstructor
public class PersonHandler {
    private final Validator validator;
    private final PersonRepository personRepository;

    Mono<ServerResponse> handleFindAll(ServerRequest serverRequest) {
        log.info("Handle request {} {}", serverRequest.method(), serverRequest.path());
        Flux<Person> people = this.personRepository.findAll();
        return ok()
                .body(people, Person.class);
    }

    Mono<ServerResponse> handleFindById(ServerRequest serverRequest) {
        var id = Long.parseLong(serverRequest.pathVariable("id"));
        log.info("Handle request {} {}", serverRequest.method(), serverRequest.path());
        Mono<Person> partnerMono = this.personRepository.findById(id);
        Mono<ServerResponse> notFound = ServerResponse
                .notFound()
                .build();
        return partnerMono
                .flatMap(partner -> ok()
                        .body(fromValue(partner)))
                .switchIfEmpty(notFound);
    }

    Mono<ServerResponse> handleDeleteById(ServerRequest serverRequest) {
        var id = Long.parseLong(serverRequest.pathVariable("id"));
        log.info("Handle request {} {}", serverRequest.method(), serverRequest.path());
        Mono<Mono<String>> monoMono = this.personRepository.findById(id)
                .flatMap(this.personRepository::delete)
                .thenReturn(Mono.just("successfully deleted!"));
        return monoMono.flatMap(resp -> ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resp, String.class));
    }



    public Mono<ServerResponse> handleFindFirstByName(ServerRequest serverRequest) {
        log.info("Handle request {} {}", serverRequest.method(), serverRequest.path());
        var name = serverRequest.pathVariable("name");
        Mono<Person> firstByName = this.personRepository.findFirstByName(name);
        Mono<ServerResponse> notFound = ServerResponse.notFound().build();
        return firstByName.flatMap(person -> ok()
                .body(fromValue(person)))
                .switchIfEmpty(notFound);
    }

    Mono<ServerResponse> handleSave(ServerRequest serverRequest) {
        Mono<Person> partnerMono = serverRequest.bodyToMono(Person.class)
                .doOnNext(this::validate);
        log.info("Handle request {} {}", serverRequest.method(), serverRequest.path());
        return ok()
                .body(fromPublisher(partnerMono.flatMap(this.personRepository::save), Person.class));
    }
    private void validate(Person person) {
        log.info("validating person -> {}", person);
        Set<ConstraintViolation<Person>> errors = validator.validate(person);
        if(!errors.isEmpty()) {
            List<String> result = errors.stream()
                    .map(this::formatError)
                    .toList();
            log.info("person {} validated - {}", person, result.toString());
            throw new ServerWebInputException(result.toString());
        }
    }

    private String formatError(ConstraintViolation<Person> error) {
        String field = StringUtils.capitalize(error.getPropertyPath().toString());
        String message = error.getMessage();
        return String.format("%s - %s", field, message);
    }
}
