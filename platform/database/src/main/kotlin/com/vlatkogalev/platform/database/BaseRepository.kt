package com.vlatkogalev.platform.database

import javax.sql.DataSource

abstract class BaseRepository(
    protected val dataSource: DataSource,
)
