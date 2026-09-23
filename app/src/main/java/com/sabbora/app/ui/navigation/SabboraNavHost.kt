package com.sabbora.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sabbora.app.data.repository.SabboraRepository
import com.sabbora.app.ui.screens.ClassesScreen
import com.sabbora.app.ui.screens.ReportScreen
import com.sabbora.app.ui.screens.RollCallScreen

private const val ARG_CLASS_ID = "classId"

object Routes {
    const val CLASSES = "classes"
    const val ROLL_CALL = "class/{$ARG_CLASS_ID}/roll-call"
    const val REPORT = "class/{$ARG_CLASS_ID}/report"

    fun rollCall(classId: String) = "class/$classId/roll-call"
    fun report(classId: String) = "class/$classId/report"
}

@Composable
fun SabboraNavHost(repository: SabboraRepository) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.CLASSES) {

        composable(Routes.CLASSES) {
            ClassesScreen(
                repository = repository,
                onOpenClass = { classId -> nav.navigate(Routes.rollCall(classId)) },
            )
        }

        composable(
            route = Routes.ROLL_CALL,
            arguments = listOf(navArgument(ARG_CLASS_ID) { type = NavType.StringType }),
        ) { entry ->
            val classId = entry.arguments?.getString(ARG_CLASS_ID) ?: return@composable
            RollCallScreen(
                repository = repository,
                classId = classId,
                onBack = { nav.popBackStack() },
                onOpenReport = { nav.navigate(Routes.report(classId)) },
            )
        }

        composable(
            route = Routes.REPORT,
            arguments = listOf(navArgument(ARG_CLASS_ID) { type = NavType.StringType }),
        ) { entry ->
            val classId = entry.arguments?.getString(ARG_CLASS_ID) ?: return@composable
            ReportScreen(
                repository = repository,
                classId = classId,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
