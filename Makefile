
infra:
	docker compose -f docker-compose.yaml down && docker compose -f docker-compose.yaml up
cleanup:
	docker-compose -f docker-compose.yaml down
