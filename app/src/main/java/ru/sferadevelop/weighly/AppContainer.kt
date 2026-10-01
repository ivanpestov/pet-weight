package ru.sferadevelop.weighly

import android.content.Context
import ru.sferadevelop.weighly.data.RoomWeightRepository
import ru.sferadevelop.weighly.data.WeighlyDatabase
import ru.sferadevelop.weighly.domain.WeightRepository

/**
 * Hand-written dependency container. Two screens and one DAO do not pay for a DI framework.
 */
class AppContainer(context: Context) {

    private val database = WeighlyDatabase.create(context)

    val weightRepository: WeightRepository = RoomWeightRepository(database.recordDao())
}
