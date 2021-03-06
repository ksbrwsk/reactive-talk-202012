package de.ksbrwsk.people;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@WebFluxTest
@Import({PersonHandler.class, PersonRouter.class})
@Log4j2
class PersonHandlerTest {

    private final static String BASE_URL = "/api/people";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    PersonRepository personRepository;

    @Test
    @DisplayName("should handle request find all")
    void should_handle_find_all() {
        when(this.personRepository.findAll())
                .thenReturn(Flux.just(
                        new Person(1L, "Name"),
                        new Person(2L, "Sabo")
                ));

        this.webTestClient
                .get()
                .uri(BASE_URL)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Person.class)
                .value(people -> {
                    assertEquals(people.get(0).getId(), 1L);
                    assertEquals(people.get(0).getName(), "Name");
                    assertEquals(people.get(1).getId(), 2L);
                    assertEquals(people.get(1).getName(), "Sabo");
                });
    }

    @Test
    @DisplayName("should handle request find by id x")
    void should_handle_find_by_id() {
        when(this.personRepository.findById(1L))
                .thenReturn(Mono.just(new Person(1L, "Name")));

        Person person = this.webTestClient
                .get()
                .uri(BASE_URL + "/1")
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Person.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(person);
        assertEquals(person.getName(), "Name");
        assertEquals(person.getId(), 1L);
    }

    @Test
    @DisplayName("should handle request find by unknown id x")
    void should_handle_find_by_unknown_id() {
        when(this.personRepository.findById(1000L))
                .thenReturn(Mono.empty());

        this.webTestClient
                .get()
                .uri(BASE_URL + "/1000")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    @DisplayName("should handle request delete by id x")
    void should_handle_delete_by_id() {
        Person person = new Person(1L, "Name");
        when(this.personRepository.findById(any(Long.class)))
                .thenReturn(Mono.just(person));
        when(this.personRepository.delete(any(Person.class)))
                .thenReturn(Mono.empty());

        this.webTestClient
                .delete()
                .uri(BASE_URL + "/1")
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(String.class)
                .value(msg -> msg.equals("successfully deleted!"));
    }

    @Test
    @DisplayName("should successfully handle request save person")
    void should_handle_save_person() {

        Person person = new Person(1L, "Name");
        Mono<Person> personMono = Mono.just(person);

        when(this.personRepository.save(person))
                .thenReturn(personMono);

        this.webTestClient
                .post()
                .uri("/api/people")
                .bodyValue(person)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Person.class)
                .isEqualTo(person);
    }

    @Test
    @DisplayName("should not successfully handle request save person - validation failed")
    void should_handle_save_person_name_is_empty() {
        this.webTestClient
                .post()
                .uri(BASE_URL)
                .bodyValue(new Person(""))
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    @DisplayName("should not successfully handle request save person - validation failed")
    void should_handle_save_person_name_greater_30_characters() {
        this.webTestClient
                .post()
                .uri(BASE_URL)
                .bodyValue(new Person("Name___greater___30___characters"))
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    @DisplayName("should not successfully handle request save person - validation failed")
    void should_handle_save_person_name_is_null() {
            this.webTestClient
                .post()
                .uri(BASE_URL)
                .bodyValue(new Person(null))
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    @DisplayName("should handle unknown URL")
    void should_handle_not_found() {
        this.webTestClient
                .get()
                .uri("/api/peple")
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    @DisplayName("should handle request find first by name")
    void should_handle_find_first_by_name() {
        when(this.personRepository.findFirstByName(any(String.class)))
                .thenReturn(Mono.just(new Person(1L, "First")));
        this.webTestClient
                .get()
                .uri(BASE_URL + "/firstByName/First")
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Person.class)
                .value(person -> person.getName().equalsIgnoreCase("first"));
    }

    @Test
    @DisplayName("should handle request find first by name not found")
    void should_handle_find_first_by_name_not_found() {
        when(this.personRepository.findFirstByName(any(String.class)))
                .thenReturn(Mono.empty());
        this.webTestClient
                .get()
                .uri(BASE_URL + "/firstByName/First")
                .exchange()
                .expectStatus()
                .isNotFound();
    }
}