package com.aldajo92.calendarswipeexample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.aldajo92.calendarswipeexample.ui.theme.CalendarSwipeExampleTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel : ViewModel() {

    val todayCalendar = Calendar.getInstance()
    val todayItemDayUIModel = todayCalendar.toItemDayUIModel()
    private val firstDayOfWeek = todayItemDayUIModel.simpleDateModel.dayOfWeekIndex

    val calendarMap = mutableMapOf<Int, List<ItemDayUIModel>>()

    private val _itemDayUIModelSelectedFlow = MutableStateFlow(todayItemDayUIModel)
    val itemDayUIModelSelectedFlow: StateFlow<ItemDayUIModel> = _itemDayUIModelSelectedFlow

    private val _dayOfWeekIndexFlow = MutableStateFlow(firstDayOfWeek)
    val dayOfWeekIndexFlow: StateFlow<Int> = _dayOfWeekIndexFlow

    private val _weekOffsetFlow = MutableStateFlow(0)
    val weekOffsetFlow: StateFlow<Int> = _weekOffsetFlow

    fun updateItemDayUIModelSelected(itemDayUIModel: ItemDayUIModel, weekOffset: Int) {
        _itemDayUIModelSelectedFlow.value = itemDayUIModel
        _dayOfWeekIndexFlow.value = itemDayUIModel.simpleDateModel.dayOfWeekIndex
        _weekOffsetFlow.value = weekOffset
    }

    fun updateByWeekIndex(dayOfWeekIndex: Int) {
        _dayOfWeekIndexFlow.value = dayOfWeekIndex
        val weekOffset = _weekOffsetFlow.value

        getItemDayUIModelFromIndex(dayOfWeekIndex, weekOffset)?.let {
            _itemDayUIModelSelectedFlow.value = it
        }
        // _weekOffsetFlow.value = weekOffset // TODO: No modify yet
    }

    fun refreshWeekMap(weekIndex: Int) {
        _weekOffsetFlow.value = weekIndex

        calendarMap.saveToMapNoDuplicate(
            (weekIndex + 1) to todayCalendar.weekItemDaysFromWeeksOffset(
                weekIndex + 1,
                todayCalendar,
                dateComparator
            )
        )
        calendarMap.saveToMapNoDuplicate(
            weekIndex to todayCalendar.weekItemDaysFromWeeksOffset(
                weekIndex, todayCalendar,
                dateComparator
            )
        )
        calendarMap.saveToMapNoDuplicate(
            (weekIndex - 1) to todayCalendar.weekItemDaysFromWeeksOffset(
                weekIndex - 1,
                todayCalendar,
                dateComparator
            )
        )
    }

    private fun getItemDayUIModelFromIndex(
        dayOfWeekIndexState: Int,
        weekIndexState: Int
    ): ItemDayUIModel? {
        return calendarMap[weekIndexState]?.get(dayOfWeekIndexState)
    }

}

class MainActivity : ComponentActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val pagerSections = daysInWeekArray

            val calendarMap = mainViewModel.calendarMap

            val dayOfWeekState by mainViewModel.dayOfWeekIndexFlow.collectAsState()
            val pagerState = rememberPagerState(dayOfWeekState)

            val dayOfWeekIndexState by mainViewModel.dayOfWeekIndexFlow.collectAsState()
            val weekOffsetState by mainViewModel.weekOffsetFlow.collectAsState()

            val itemDayUIModelSelected by mainViewModel.itemDayUIModelSelectedFlow.collectAsState()

            CalendarSwipeExampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val localCoroutine = rememberCoroutineScope()

                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }.collect { page ->
                            mainViewModel.updateByWeekIndex(page)
                        }
                    }

                    Column(Modifier.fillMaxSize()) {
                        CalendarHeaderComponent(
                            modifier = Modifier.fillMaxWidth(),
                            calendarWeekMap = calendarMap,
                            weekIndexChanged = {
                                mainViewModel.refreshWeekMap(it)
                            },
                            itemDayUIModelSelected = itemDayUIModelSelected
                        ) { itemDayUIModelSelected, weekOffset ->
                            mainViewModel.updateItemDayUIModelSelected(
                                itemDayUIModelSelected,
                                weekOffset
                            )

                            localCoroutine.launch {
                                pagerState.animateScrollToPage(itemDayUIModelSelected.simpleDateModel.dayOfWeekIndex)
                            }

                        }

                        HorizontalPager(
                            modifier = Modifier.weight(1f),
                            count = pagerSections.size,
                            state = pagerState
                        ) { currentPage ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(5.dp)
                                    .background(MaterialTheme.colors.background)
                                    .border(2.dp, MaterialTheme.colors.onSurface)
                            ) {
                                Text(
                                    modifier = Modifier.align(Alignment.Center),
                                    text = currentPage.toString()
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "weekIndex: $weekOffsetState")
                            Text(text = "dayOfWeekIndex: $dayOfWeekIndexState")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSnapperApi::class)
@Composable
fun CalendarHeaderComponent(
    modifier: Modifier = Modifier,
    calendarWeekMap: Map<Int, List<ItemDayUIModel>> = mutableMapOf(),
    itemDayUIModelSelected: ItemDayUIModel = ItemDayUIModel(),
    weekIndexChanged: (Int) -> Unit = {},
    itemDayClickedEvent: (ItemDayUIModel, Int) -> Unit = { _, _ -> }
) {
    val listState = rememberLazyListState(Int.MAX_VALUE / 2)

    //    TODO: Use this to handle week change.
    //    LaunchedEffect(key1 = listState) {
    //        CoroutineScope(Dispatchers.IO).launch {
    //            delay(5000)
    //            withContext(localCoroutine.coroutineContext) {
    //                listState.animateScrollToItem(Int.MAX_VALUE / 2 - 1)
    //            }
    //        }
    //    }

    LazyRow(
        state = listState,
        modifier = modifier,
        flingBehavior = rememberSnapperFlingBehavior(listState),
    ) {
        val weekOffsetIndex = listState.firstVisibleItemIndex - (Int.MAX_VALUE / 2)
        weekIndexChanged(weekOffsetIndex)

        items(Int.MAX_VALUE, itemContent = { relativeIndex: Int ->
            val absoluteIndex = relativeIndex - (Int.MAX_VALUE / 2)
            WeekRowCalendarComponent(
                listItemDaysUI = calendarWeekMap[absoluteIndex] ?: listOf(),
                itemDayUIModelSelected = itemDayUIModelSelected,
                itemDayClickedEvent = { itemDayClickedEvent(it, weekOffsetIndex) }
            )
        })
    }
}

@Preview
@Composable
fun WeekRowCalendarComponent(
    modifier: Modifier = Modifier,
    listItemDaysUI: List<ItemDayUIModel> = (0..6).map {
        ItemDayUIModel(
            simpleDateModel = SimpleDateModel(dayOfWeekIndex = it),
            numberDay = "$it",
            textDay = daysInWeekArray[it]
        )
    },
    itemDayUIModelSelected: ItemDayUIModel = ItemDayUIModel(),
    itemDayClickedEvent: (ItemDayUIModel) -> Unit = {}
) {
    val context = LocalContext.current
    val resources = context.resources
    val displayMetrics = resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels / displayMetrics.density

    Row(modifier.width(screenWidth.dp)) {
        listItemDaysUI.map {
            ItemDayComponent(
                modifier = Modifier.weight(1f),
                itemDayUIModel = it,
                showCircleBackground = it.markDate,
                selectedItem = it.equalsInSimpleDate(itemDayUIModelSelected),
                itemDayClickedEvent = itemDayClickedEvent
            )
        }
    }
}

@Preview(widthDp = 80)
@Composable
fun ItemDayComponent(
    modifier: Modifier = Modifier,
    itemDayUIModel: ItemDayUIModel = ItemDayUIModel(),
    showCircleBackground: Boolean = true,
    selectedItem: Boolean = false,
    itemDayClickedEvent: (ItemDayUIModel) -> Unit = {}
) {
    Column(modifier = modifier.clickable { itemDayClickedEvent(itemDayUIModel) }) {
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = itemDayUIModel.textDay)
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .height(IntrinsicSize.Min)
        ) {
            if (selectedItem) {
                Spacer(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            } else {
                if (showCircleBackground) Spacer(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Gray)
                )
            }
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = itemDayUIModel.numberDay,
                fontSize = 20.sp
            )
        }
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = itemDayUIModel.captionText
        )
    }
}

////////////////////////////////////////////////////////////////////////////
fun Calendar.getDayOfWeekIndex(): Int {
    val dayOfWeekFromCalendar = this.get(Calendar.DAY_OF_WEEK) + 7
    return (dayOfWeekFromCalendar - 2) % 7
}

fun Calendar.getDayOfMonth(): Int {
    return get(Calendar.DAY_OF_MONTH)
}

fun Calendar.getMonth(): Int {
    return get(Calendar.MONTH)
}

fun Calendar.getYear(): Int {
    return get(Calendar.YEAR)
}

fun Calendar.addDays(days: Int): Calendar = Calendar.getInstance().apply {
    this.add(Calendar.DATE, days)
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
): List<ItemDayUIModel> {
    val localCalendar = this.clone() as Calendar
    localCalendar.add(Calendar.DATE, 7 * index)

    val dayOfWeek = localCalendar.get(Calendar.DAY_OF_WEEK)

    val calendarNearMonday = localCalendar.clone() as Calendar
    calendarNearMonday.add(Calendar.DATE, -dayOfWeek + 2)

    return (0..6).map {
        val tmpCalendar = calendarNearMonday.clone() as Calendar
        tmpCalendar.add(Calendar.DATE, it)
        val dayNumberInMonth = tmpCalendar.get(Calendar.DAY_OF_MONTH)
        ItemDayUIModel(
            simpleDateModel = tmpCalendar.toSimpleDateModel(),
            numberDay = "$dayNumberInMonth",
            textDay = daysInWeekArray[it],
            markDate = comparativeDay(currentCalendar, tmpCalendar)
        )
    }
}

fun Calendar.toSimpleDateModel() = SimpleDateModel(
    dayOfWeekIndex = this.getDayOfWeekIndex(),
    dayOfMonth = this.getDayOfMonth(),
    month = this.getMonth(),
    year = this.getYear()
)

fun Calendar.toItemDayUIModel() = this.toSimpleDateModel().let {
    ItemDayUIModel(
        simpleDateModel = it,
        numberDay = it.dayOfMonth.toString(),
        textDay = daysInWeekArray[it.dayOfWeekIndex]
    )
}

fun ItemDayUIModel.equalsInSimpleDate(itemDayUIModel: ItemDayUIModel) =
    this.simpleDateModel == itemDayUIModel.simpleDateModel

fun SimpleDateModel.toCalendar() = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, this@toCalendar.dayOfMonth)
    set(Calendar.MONTH, this@toCalendar.month)
    set(Calendar.YEAR, this@toCalendar.year)
}

////////////////////////////////////////////////////////////////////////////

val daysInWeekArray = arrayOf("M", "T", "W", "T", "F", "S", "S")

val dateComparator = { today: Calendar, dayToRender: Calendar ->
    today.timeInMillis >= dayToRender.timeInMillis
}

////////////////////////////////////////////////////////////////////////////

data class ItemDayUIModel(
    val simpleDateModel: SimpleDateModel = SimpleDateModel(),
    val numberDay: String = "01",
    val textDay: String = "M",
    val captionText: String = "0.00",
    val markDate: Boolean = false
)

data class SimpleDateModel(
    val dayOfWeekIndex: Int = 0,
    val dayOfMonth: Int = 0,
    val month: Int = 0,
    val year: Int = 0
)

////////////////////////////////////////////////////////

