package sg.gov.tech.bluetrace.favourite

import sg.gov.tech.bluetrace.favourite.persistence.FavouriteRecord

data class FavouritesAdapterListModel(
    var favRecords: FavouriteRecord,
    var isChecked: Boolean = true
)
