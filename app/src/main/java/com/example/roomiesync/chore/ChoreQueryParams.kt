package com.example.roomiesync.chore

import com.example.roomiesync.ui.components.ChoreStatus

enum class SortField {
    CHORE_NAME,
    TIME_UNTIL_DUE
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

data class ChoreQueryParams(
    val sortBy: SortField? = SortField.TIME_UNTIL_DUE,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val statuses: Set<ChoreStatus> = emptySet(),
    val assignedToMeOnly: Boolean = false,
    val dueDateStart: Long? = null,
    val dueDateEnd: Long? = null
)
