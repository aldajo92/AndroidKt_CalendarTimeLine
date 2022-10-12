package com.aldajo92.calendarswipeexample

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aldajo92.calendarswipeexample.ui.theme.CalendarSwipeExampleTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel : ViewModel() {

    val todayCalendar = Calendar.getInstance()

    private val _itemDayUIModelSelectedLiveData = MutableLiveData<ItemDayUIModel>()
    val itemDayUIModelSelected: LiveData<ItemDayUIModel> = _itemDayUIModelSelectedLiveData

    fun updateItemDayUIModelSelected(itemDayUIModel: ItemDayUIModel) {
        _itemDayUIModelSelectedLiveData.value = itemDayUIModel
    }


}

class MainActivity : ComponentActivity() {

    val mainViewModel by viewModels<MainViewModel>()

    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalendarSwipeExampleTheme {

                val localCoroutine = rememberCoroutineScope()
                val todayCalendar = remember { Calendar.getInstance() }
                val pagerSections = daysInWeekArray
                val pagerState = rememberPagerState(todayCalendar.getDayOfWeekIndex())

                val calendarMap = remember { mutableMapOf<Int, List<ItemDayUIModel>>() }

                val itemDayUIModelSelected =
                    remember { mutableStateOf(ItemDayUIModel(simpleDateModel = todayCalendar.toSimpleDateModel())) }

                Column(Modifier.fillMaxSize()) {

                    CalendarHeaderComponent(
                        modifier = Modifier.fillMaxWidth(),
                        calendarWeekMap = calendarMap,
                        todayCalendar = todayCalendar,
                        itemDayUIModelSelected = itemDayUIModelSelected.value
                    ) {
                        itemDayUIModelSelected.value = it
                        Log.d("ADJ Index", it.simpleDateModel.dayOfWeekIndex.toString())
                        localCoroutine.launch {
                            pagerState.animateScrollToPage(itemDayUIModelSelected.value.simpleDateModel.dayOfWeekIndex)
                        }
                    }

                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }.collect { page ->
                            Log.d("ADJ Swipe", page.toString())
//                            itemDayUIModelSelected.value = itemDayUIModelSelected.value.let {
//                                val tmpSimpleDateModel =
//                                    it.simpleDateModel.copy(dayOfWeekIndex = currentPageIndex)
//                                it.copy(simpleDateModel = tmpSimpleDateModel)
//                            }
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
                                .background(Color.Gray)
                        ) {
                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = currentPage.toString()
                            )
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
    calendarWeekMap: MutableMap<Int, List<ItemDayUIModel>> = mutableMapOf(), // Replace MutableMap by Map
    todayCalendar: Calendar = Calendar.getInstance(),
    itemDayUIModelSelected: ItemDayUIModel = ItemDayUIModel(),
    itemDayClickedEvent: (ItemDayUIModel) -> Unit = {}
) {
    val listState = rememberLazyListState(Int.MAX_VALUE / 2)

    val index = listState.firstVisibleItemIndex - (Int.MAX_VALUE / 2)

    calendarWeekMap.saveToMapNoDuplicate(
        (index + 1) to todayCalendar.weekItemDaysFromWeeksOffset(
            index + 1,
            todayCalendar,
            dateComparator
        )
    )
    calendarWeekMap.saveToMapNoDuplicate(
        index to todayCalendar.weekItemDaysFromWeeksOffset(
            index, todayCalendar,
            dateComparator
        )
    )
    calendarWeekMap.saveToMapNoDuplicate(
        (index - 1) to todayCalendar.weekItemDaysFromWeeksOffset(
            index - 1,
            todayCalendar,
            dateComparator
        )
    )

    LazyRow(
        state = listState,
        modifier = modifier,
        flingBehavior = rememberSnapperFlingBehavior(listState),
    ) {
        items(Int.MAX_VALUE, itemContent = { relativeIndex: Int ->
            val absoluteIndex = relativeIndex - (Int.MAX_VALUE / 2)
            WeekRowCalendarComponent(
                listItemDaysUI = calendarWeekMap[absoluteIndex] ?: listOf(),
                itemDayUIModelSelected = itemDayUIModelSelected,
                itemDayClickedEvent = itemDayClickedEvent
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

fun ItemDayUIModel.equalsInSimpleDate(itemDayUIModel: ItemDayUIModel) =
    this.simpleDateModel == itemDayUIModel.simpleDateModel
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
