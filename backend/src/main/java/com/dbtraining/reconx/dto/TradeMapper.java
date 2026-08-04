package com.dbtraining.reconx.dto;

import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.TradeStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * ============================================================================
 * TICKET-ADV054 — MapStruct mapper: Trade entity <-> DTO
 *
 * WHAT:    Generates the entity↔DTO conversion at compile time.
 * HOW:     componentModel="spring" → MapStruct emits a @Component bean named
 *          tradeMapper that you can @Autowire. unmappedTargetPolicy=ERROR
 *          fails the build the moment a field is added to one side and
 *          forgotten on the other.
 * WHY:     Hand-written mappers drift. MapStruct fails the build if a new
 *          field is added to one side and forgotten on the other.
 * ============================================================================
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TradeMapper {

    @Mapping(source = "instrument.id", target = "instrumentId")
    @Mapping(source = "instrument.symbol", target = "instrumentSymbol")
    @Mapping(source = "instrument.isin", target = "isin")
    @Mapping(source = "counterparty.id", target = "counterpartyId")
    @Mapping(source = "counterparty.name", target = "counterpartyName")
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    TradeResponse toResponse(Trade trade);

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "counterparty", ignore = true) // wired by the service from counterpartyId
    @Mapping(target = "instrument",   ignore = true) // wired by the service from instrumentId
    @Mapping(target = "status",      ignore = true) // defaulted to PENDING by the entity
    @Mapping(target = "deletedAt",   ignore = true)
    @Mapping(target = "createdAt",   ignore = true) // populated by Spring Data auditing
    @Mapping(target = "modifiedAt",  ignore = true) // populated by Spring Data auditing
    Trade toEntity(TradeRequest request);

    @Named("statusToString")
    static String statusToString(TradeStatus status) {
        return status == null ? null : status.name();
    }
}
