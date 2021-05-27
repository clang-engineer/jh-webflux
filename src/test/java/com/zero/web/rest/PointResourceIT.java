package com.zero.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.zero.IntegrationTest;
import com.zero.domain.Point;
import com.zero.repository.PointRepository;
import com.zero.service.EntityManager;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for the {@link PointResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient
@WithMockUser
class PointResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAAAAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBBBBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/points";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Point point;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Point createEntity(EntityManager em) {
        Point point = new Point().title(DEFAULT_TITLE).description(DEFAULT_DESCRIPTION);
        return point;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Point createUpdatedEntity(EntityManager em) {
        Point point = new Point().title(UPDATED_TITLE).description(UPDATED_DESCRIPTION);
        return point;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Point.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @AfterEach
    public void cleanup() {
        deleteEntities(em);
    }

    @BeforeEach
    public void initTest() {
        deleteEntities(em);
        point = createEntity(em);
    }

    @Test
    void createPoint() throws Exception {
        int databaseSizeBeforeCreate = pointRepository.findAll().collectList().block().size();
        // Create the Point
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(point))
            .exchange()
            .expectStatus()
            .isCreated();

        // Validate the Point in the database
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeCreate + 1);
        Point testPoint = pointList.get(pointList.size() - 1);
        assertThat(testPoint.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testPoint.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    }

    @Test
    void createPointWithExistingId() throws Exception {
        // Create the Point with an existing ID
        point.setId(1L);

        int databaseSizeBeforeCreate = pointRepository.findAll().collectList().block().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(point))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Point in the database
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    void checkTitleIsRequired() throws Exception {
        int databaseSizeBeforeTest = pointRepository.findAll().collectList().block().size();
        // set the field null
        point.setTitle(null);

        // Create the Point, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(point))
            .exchange()
            .expectStatus()
            .isBadRequest();

        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    void getAllPointsAsStream() {
        // Initialize the database
        pointRepository.save(point).block();

        List<Point> pointList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(Point.class)
            .getResponseBody()
            .filter(point::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(pointList).isNotNull();
        assertThat(pointList).hasSize(1);
        Point testPoint = pointList.get(0);
        assertThat(testPoint.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testPoint.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    }

    @Test
    void getAllPoints() {
        // Initialize the database
        pointRepository.save(point).block();

        // Get all the pointList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(point.getId().intValue()))
            .jsonPath("$.[*].title")
            .value(hasItem(DEFAULT_TITLE))
            .jsonPath("$.[*].description")
            .value(hasItem(DEFAULT_DESCRIPTION));
    }

    @Test
    void getPoint() {
        // Initialize the database
        pointRepository.save(point).block();

        // Get the point
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, point.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(point.getId().intValue()))
            .jsonPath("$.title")
            .value(is(DEFAULT_TITLE))
            .jsonPath("$.description")
            .value(is(DEFAULT_DESCRIPTION));
    }

    @Test
    void getNonExistingPoint() {
        // Get the point
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putNewPoint() throws Exception {
        // Initialize the database
        pointRepository.save(point).block();

        int databaseSizeBeforeUpdate = pointRepository.findAll().collectList().block().size();

        // Update the point
        Point updatedPoint = pointRepository.findById(point.getId()).block();
        updatedPoint.title(UPDATED_TITLE).description(UPDATED_DESCRIPTION);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedPoint.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(updatedPoint))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Point in the database
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeUpdate);
        Point testPoint = pointList.get(pointList.size() - 1);
        assertThat(testPoint.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testPoint.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
    }

    @Test
    void putNonExistingPoint() throws Exception {
        int databaseSizeBeforeUpdate = pointRepository.findAll().collectList().block().size();
        point.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, point.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(point))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Point in the database
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchPoint() throws Exception {
        int databaseSizeBeforeUpdate = pointRepository.findAll().collectList().block().size();
        point.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(point))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Point in the database
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamPoint() throws Exception {
        int databaseSizeBeforeUpdate = pointRepository.findAll().collectList().block().size();
        point.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(point))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Point in the database
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdatePointWithPatch() throws Exception {
        // Initialize the database
        pointRepository.save(point).block();

        int databaseSizeBeforeUpdate = pointRepository.findAll().collectList().block().size();

        // Update the point using partial update
        Point partialUpdatedPoint = new Point();
        partialUpdatedPoint.setId(point.getId());

        partialUpdatedPoint.title(UPDATED_TITLE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedPoint.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedPoint))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Point in the database
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeUpdate);
        Point testPoint = pointList.get(pointList.size() - 1);
        assertThat(testPoint.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testPoint.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
    }

    @Test
    void fullUpdatePointWithPatch() throws Exception {
        // Initialize the database
        pointRepository.save(point).block();

        int databaseSizeBeforeUpdate = pointRepository.findAll().collectList().block().size();

        // Update the point using partial update
        Point partialUpdatedPoint = new Point();
        partialUpdatedPoint.setId(point.getId());

        partialUpdatedPoint.title(UPDATED_TITLE).description(UPDATED_DESCRIPTION);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedPoint.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedPoint))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Point in the database
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeUpdate);
        Point testPoint = pointList.get(pointList.size() - 1);
        assertThat(testPoint.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testPoint.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
    }

    @Test
    void patchNonExistingPoint() throws Exception {
        int databaseSizeBeforeUpdate = pointRepository.findAll().collectList().block().size();
        point.setId(count.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, point.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(point))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Point in the database
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchPoint() throws Exception {
        int databaseSizeBeforeUpdate = pointRepository.findAll().collectList().block().size();
        point.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, count.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(point))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Point in the database
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamPoint() throws Exception {
        int databaseSizeBeforeUpdate = pointRepository.findAll().collectList().block().size();
        point.setId(count.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(point))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Point in the database
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void deletePoint() {
        // Initialize the database
        pointRepository.save(point).block();

        int databaseSizeBeforeDelete = pointRepository.findAll().collectList().block().size();

        // Delete the point
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, point.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        List<Point> pointList = pointRepository.findAll().collectList().block();
        assertThat(pointList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
