package com.aldajo92.calendarswipeexample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aldajo92.calendarswipeexample.ui.theme.CalendarSwipeExampleTheme
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalendarSwipeExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
//                    Greeting("Android")
                    val list = (1..3).map { "item $it" }
                    CircularList(list, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CalendarSwipeExampleTheme {
        Greeting("Android")
    }
}

@OptIn(ExperimentalSnapperApi::class)
@Composable
fun CircularList(
    items: List<String>,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit = {}
) {
    val calendarMap = remember { mutableMapOf<Int, List<ItemDayUI>>() }
    val todayCalendar = remember { Calendar.getInstance() }
    val listState = rememberLazyListState(Int.MAX_VALUE / 2)

    val context = LocalContext.current
    val resources = context.resources
    val displayMetrics = resources.displayMetrics
    // Compute the screen width using the actual display width and the density of the display.
    val screenWidth = displayMetrics.widthPixels / displayMetrics.density
//    LazyRow(
//        state = listState,
//        modifier = modifier,
//        flingBehavior = rememberSnapperFlingBehavior(listState),
//    ) {
//        items(Int.MAX_VALUE, itemContent = {
//            val index = it % items.size
//            Card(Modifier.width(screenWidth.dp)) {
//                Box(Modifier.fillMaxSize()) {
//                    Text(modifier = Modifier.align(Alignment.Center), text = items[index])
//                }
//            }
//        })
//    }

    val index = listState.firstVisibleItemIndex - (Int.MAX_VALUE / 2)

    calendarMap.saveToMapNoDuplicate(
        (index + 1) to todayCalendar.weekItemDaysFromWeeksOffset(
            index + 1,
            todayCalendar,
            dateComparator
        )
    )
    calendarMap.saveToMapNoDuplicate(
        index to todayCalendar.weekItemDaysFromWeeksOffset(
            index, todayCalendar,
            dateComparator
        )
    )
    calendarMap.saveToMapNoDuplicate(
        (index - 1) to todayCalendar.weekItemDaysFromWeeksOffset(
            index - 1,
            todayCalendar,
            dateComparator
        )
    )

    Log.d("AlejandroGomez", "$index")

    LazyRow(
        state = listState,
        modifier = modifier,
        flingBehavior = rememberSnapperFlingBehavior(listState),
    ) {
        items(Int.MAX_VALUE, itemContent = { relativeIndex: Int ->
            val absoluteIndex = relativeIndex - (Int.MAX_VALUE / 2)
            WeekRowCalendarComponent(
                listItemDaysUI = calendarMap[absoluteIndex] ?: listOf()
            )
        })
    }
}

@Preview
@Composable
fun WeekRowCalendarComponent(
    modifier: Modifier = Modifier,
    listItemDaysUI: List<ItemDayUI> = (0..6).map {
        ItemDayUI(numberDay = "$it", textDay = daysInWeek[it])
    }
) {
    val context = LocalContext.current
    val resources = context.resources
    val displayMetrics = resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels / displayMetrics.density

    Row(modifier.width(screenWidth.dp)) {
        listItemDaysUI.map {
            ItemDayComponent(
                modifier = Modifier.weight(1f),
                numberDay = it.numberDay,
                textDay = it.textDay,
                showCircleBackground = it.markDate
            )
        }
    }
}

@Preview
@Composable
fun ItemDayComponent(
    modifier: Modifier = Modifier,
    numberDay: String = "01",
    textDay: String = "M",
    captionText: String = "0.00",
    showCircleBackground: Boolean = true
) {
    Column(modifier = modifier) {
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = textDay)
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .height(IntrinsicSize.Min)
        ) {
            if (showCircleBackground) Spacer(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = numberDay,
                fontSize = 20.sp
            )
        }
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = captionText)
    }
}

////////////////////////////////////////////////////////////////////////////

val dateComparator = { today: Calendar, dayToRender: Calendar ->
    today.timeInMillis >= dayToRender.timeInMillis
}

fun <K, V> MutableMap<K, V>.saveToMapNoDuplicate(entry: Pair<K, V>) {
    if (!this.containsKey(entry.first)) {
        this[entry.first] = entry.second
    }
}

fun Calendar.weekItemDaysFromWeeksOffset(
    index: Int,
    currentCalendar: Calendar = Calendar.getInstance(),
    comparativeDay: (Calendar, Calendar) -> Boolean = { _, _ -> false }
): List<ItemDayUI> {
    val localCalendar = this.clone() as Calendar
    localCalendar.add(Calendar.DATE, 7 * index)

    val dayOfMonth = localCalendar.get(Calendar.DAY_OF_MONTH)
    val dayOfWeek = localCalendar.get(Calendar.DAY_OF_WEEK)

    val calendarNearMonday = localCalendar.clone() as Calendar
    calendarNearMonday.add(Calendar.DATE, -dayOfWeek + 2)
    val nearMondayDayOfMonth = calendarNearMonday.get(Calendar.DAY_OF_MONTH)

    return (0..6).map {
        val tmpCalendar = calendarNearMonday.clone() as Calendar
        tmpCalendar.add(Calendar.DATE, it)
        val dayNumberInMonth = tmpCalendar.get(Calendar.DAY_OF_MONTH)
        ItemDayUI(
            numberDay = "$dayNumberInMonth",
            textDay = daysInWeek[it],
            markDate = comparativeDay(currentCalendar, tmpCalendar)
        )
    }
}
////////////////////////////////////////////////////////////////////////////

val daysInWeek = arrayOf("M", "T", "W", "T", "F", "S", "S")

data class ItemDayUI(
    val numberDay: String = "01",
    val textDay: String = "M",
    val captionText: String = "0.00",
    val markDate: Boolean = false
)

//@OptIn(ExperimentalPagerApi::class)
//@Composable
//fun CircularList(
//    items: List<String>,
//    modifier: Modifier = Modifier,
//    onItemClick: (String) -> Unit = {}
//) {
//    val pagerState = rememberPagerState(
//        pageCount = 4,
//        initialPage = 4/2
////        initialOffscreenLimit = 4 / 2
//    )
//
//    HorizontalPager(modifier = modifier, state = pagerState) { page ->
//        // Our page content
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center,
//            modifier = Modifier.fillMaxSize()
//        ) {
//            Text(
//                text = "Item: $page",
//                textAlign = TextAlign.Center,
//                modifier = Modifier.fillMaxWidth()
//            )
//            Text(
//                text = "Something Else",
//                textAlign = TextAlign.Center,
//                modifier = Modifier.fillMaxWidth()
//            )
//            //Your other composable
//        }
//    }
//}


////////////////////////////////////////////////////////

