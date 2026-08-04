package com.dbtraining.reconx.repository.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * TICKET-ADV051 — JPA entity Instrument. JSONB metadata column wired via
 * the Hypersistence Utils JsonBinaryType on Postgres; H2 stores it as a
 * plain CLOB via the dialect translation (acceptable for dev).
 *
 * KNOWN LIMITATION: reading this column back through JPA against H2
 * specifically (the `dev` profile) throws "cannot be transformed to Json
 * object" — H2's JDBC driver returns the JSON text quoted (`"{}"` instead
 * of Postgres's unquoted `{}`), which both this library's parser and
 * Hibernate 6's own native @JdbcTypeCode(SqlTypes.JSON) mapping reject the
 * same way (tried and reverted — same underlying failure either way, this
 * is a JDBC-driver-level representation quirk, not a library choice).
 * A real fix needs a custom H2-specific JavaType/JdbcType pair that strips
 * the extra quoting, or avoiding JSON columns on H2 entirely (e.g. a
 * profile-conditional plain-text mapping) — out of scope for this pass.
 * The Testcontainers/Postgres path this project actually tests against is
 * unaffected; only local `dev` (H2) runs hit this on any endpoint that
 * loads an Instrument through JPA (e.g. creating or updating a trade).
 */
@Entity
@Table(name = "instruments")
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private InstrumentAssetClass assetClass;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 12)
    private String isin;

    /**
     * Tick size, lot size, exchange code, etc. On H2 (dev profile) this
     * stores as a CLOB; on Postgres it's true JSONB, queryable via @>.
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    public Instrument() {}

    public Long getId()                    { return id; }
    public String getSymbol()              { return symbol; }
    public String getName()                { return name; }
    public InstrumentAssetClass getAssetClass() { return assetClass; }
    public String getCurrency()            { return currency; }
    public String getIsin()                { return isin; }
    public Map<String, Object> getMetadata() { return metadata; }

    public void setSymbol(String v)               { this.symbol = v; }
    public void setName(String v)                 { this.name = v; }
    public void setAssetClass(InstrumentAssetClass v) { this.assetClass = v; }
    public void setCurrency(String v)             { this.currency = v; }
    public void setIsin(String v)                 { this.isin = v; }
    public void setMetadata(Map<String, Object> v) { this.metadata = v; }
}
