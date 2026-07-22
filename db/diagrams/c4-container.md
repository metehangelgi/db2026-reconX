C4Container

    title C4 Container — ReconX

 

    Person(user, "User", "Trader / Analyst / Admin")

    System_Ext(omsKafka, "Internal OMS", "Upstream trade source")

    System_Ext(sso, "Corporate SSO", "OIDC IdP")

 

    System_Boundary(reconxBoundary, "ReconX") {

 

        Container(reactSpa, "Recon UI", "React 19 + Vite", "SPA")

        Container(api, "recon-service API", "Spring Boot", "REST API")

        Container(reconEngine, "Reconciliation Engine", "Spring", "Matching logic")

        ContainerDb(postgres, "PostgreSQL 16", "Database", "Trade store")

        ContainerQueue(kafka, "Apache Kafka", "Streaming", "Events")

        Container(prom, "Prometheus", "Metrics", "Monitoring")

        Container(graf, "Grafana", "Dashboards", "Visualization")

    }

 

    Rel(user, reactSpa, "Uses", "HTTPS")

    Rel(reactSpa, api, "REST + SSE", "HTTPS / JSON")

    Rel(reactSpa, sso, "Login (OIDC)", "HTTPS")

    Rel(api, postgres, "Reads + writes", "JDBC")

    Rel(api, kafka, "Publishes trade-events", "Kafka")

    Rel(reconEngine, kafka, "Consumes trade-events", "Kafka")

    Rel(reconEngine, postgres, "Writes recon_breaks", "JDBC")

    Rel(omsKafka, kafka, "Streams trades", "Kafka MirrorMaker")

    Rel(prom, api, "Scrapes metrics", "HTTPS")

    Rel(graf, prom, "Queries metrics", "PromQL")