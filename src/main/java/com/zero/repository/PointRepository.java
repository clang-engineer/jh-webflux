package com.zero.repository;

import com.zero.domain.Point;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data SQL reactive repository for the Point entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PointRepository extends R2dbcRepository<Point, Long>, PointRepositoryInternal {
    // just to avoid having unambigous methods
    @Override
    Flux<Point> findAll();

    @Override
    Mono<Point> findById(Long id);

    @Override
    <S extends Point> Mono<S> save(S entity);
}

interface PointRepositoryInternal {
    <S extends Point> Mono<S> insert(S entity);
    <S extends Point> Mono<S> save(S entity);
    Mono<Integer> update(Point entity);

    Flux<Point> findAll();
    Mono<Point> findById(Long id);
    Flux<Point> findAllBy(Pageable pageable);
    Flux<Point> findAllBy(Pageable pageable, Criteria criteria);
}
