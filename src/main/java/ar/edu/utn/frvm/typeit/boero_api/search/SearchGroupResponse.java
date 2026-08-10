package ar.edu.utn.frvm.typeit.boero_api.search;

import java.util.List;

public record SearchGroupResponse(
    SearchEntityType entityType, List<SearchResultResponse> items, boolean hasMore) {}
