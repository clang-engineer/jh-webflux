package com.zero.web.rest;

import com.zero.domain.Point;
import com.zero.repository.PointRepository;
import com.zero.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link com.zero.domain.Point}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class PointResource {

    private final Logger log = LoggerFactory.getLogger(PointResource.class);

    private static final String ENTITY_NAME = "point";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PointRepository pointRepository;

    public PointResource(PointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    /**
     * {@code POST  /points} : Create a new point.
     *
     * @param point the point to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new point, or with status {@code 400 (Bad Request)} if the point has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/points")
    public Mono<ResponseEntity<Point>> createPoint(@Valid @RequestBody Point point) throws URISyntaxException {
        log.debug("REST request to save Point : {}", point);
        if (point.getId() != null) {
            throw new BadRequestAlertException("A new point cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return pointRepository
            .save(point)
            .map(
                result -> {
                    try {
                        return ResponseEntity
                            .created(new URI("/api/points/" + result.getId()))
                            .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    /**
     * {@code PUT  /points/:id} : Updates an existing point.
     *
     * @param id the id of the point to save.
     * @param point the point to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated point,
     * or with status {@code 400 (Bad Request)} if the point is not valid,
     * or with status {@code 500 (Internal Server Error)} if the point couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/points/{id}")
    public Mono<ResponseEntity<Point>> updatePoint(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Point point
    ) throws URISyntaxException {
        log.debug("REST request to update Point : {}, {}", id, point);
        if (point.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, point.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return pointRepository
            .existsById(id)
            .flatMap(
                exists -> {
                    if (!exists) {
                        return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                    }

                    return pointRepository
                        .save(point)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                        .map(
                            result ->
                                ResponseEntity
                                    .ok()
                                    .headers(
                                        HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString())
                                    )
                                    .body(result)
                        );
                }
            );
    }

    /**
     * {@code PATCH  /points/:id} : Partial updates given fields of an existing point, field will ignore if it is null
     *
     * @param id the id of the point to save.
     * @param point the point to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated point,
     * or with status {@code 400 (Bad Request)} if the point is not valid,
     * or with status {@code 404 (Not Found)} if the point is not found,
     * or with status {@code 500 (Internal Server Error)} if the point couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/points/{id}", consumes = "application/merge-patch+json")
    public Mono<ResponseEntity<Point>> partialUpdatePoint(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Point point
    ) throws URISyntaxException {
        log.debug("REST request to partial update Point partially : {}, {}", id, point);
        if (point.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, point.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return pointRepository
            .existsById(id)
            .flatMap(
                exists -> {
                    if (!exists) {
                        return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                    }

                    Mono<Point> result = pointRepository
                        .findById(point.getId())
                        .map(
                            existingPoint -> {
                                if (point.getTitle() != null) {
                                    existingPoint.setTitle(point.getTitle());
                                }
                                if (point.getDescription() != null) {
                                    existingPoint.setDescription(point.getDescription());
                                }

                                return existingPoint;
                            }
                        )
                        .flatMap(pointRepository::save);

                    return result
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                        .map(
                            res ->
                                ResponseEntity
                                    .ok()
                                    .headers(
                                        HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, res.getId().toString())
                                    )
                                    .body(res)
                        );
                }
            );
    }

    /**
     * {@code GET  /points} : get all the points.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of points in body.
     */
    @GetMapping("/points")
    public Mono<List<Point>> getAllPoints() {
        log.debug("REST request to get all Points");
        return pointRepository.findAll().collectList();
    }

    /**
     * {@code GET  /points} : get all the points as a stream.
     * @return the {@link Flux} of points.
     */
    @GetMapping(value = "/points", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Point> getAllPointsAsStream() {
        log.debug("REST request to get all Points as a stream");
        return pointRepository.findAll();
    }

    /**
     * {@code GET  /points/:id} : get the "id" point.
     *
     * @param id the id of the point to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the point, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/points/{id}")
    public Mono<ResponseEntity<Point>> getPoint(@PathVariable Long id) {
        log.debug("REST request to get Point : {}", id);
        Mono<Point> point = pointRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(point);
    }

    /**
     * {@code DELETE  /points/:id} : delete the "id" point.
     *
     * @param id the id of the point to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/points/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> deletePoint(@PathVariable Long id) {
        log.debug("REST request to delete Point : {}", id);
        return pointRepository
            .deleteById(id)
            .map(
                result ->
                    ResponseEntity
                        .noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                        .build()
            );
    }
}
