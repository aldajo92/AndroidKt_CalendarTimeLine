package com.aldajo92.calendarswipeexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.CalendarView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs
import kotlin.math.sign

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

    fun updateDataFromExternalItem(externalItemDayUIModel: ItemDayUIModel, weekOffset: Int) {
        refreshWeekMap(weekOffset)
        updateItemDayUIModelSelected(
            externalItemDayUIModel,
            weekOffset
        )
    }

}

class MainActivity : ComponentActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    @SuppressLint("CoroutineCreationDuringComposition")
    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val calendarMap = mainViewModel.calendarMap

            val dayOfWeekState by mainViewModel.dayOfWeekIndexFlow.collectAsState()
            val pagerState = rememberPagerState(dayOfWeekState)
            val listState = rememberLazyListState(Int.MAX_VALUE / 2)

            val dayOfWeekIndexState by mainViewModel.dayOfWeekIndexFlow.collectAsState()
            val weekOffsetState by mainViewModel.weekOffsetFlow.collectAsState()

            val itemDayUIModelSelected by mainViewModel.itemDayUIModelSelectedFlow.collectAsState()

            val coroutineScope = rememberCoroutineScope()

            MainUI(
                calendarMap,
                pagerState,
                listState,
                dayOfWeekIndexState,
                weekOffsetState,
                itemDayUIModelSelected,
                updateWeekEvent = {
                    mainViewModel.updateByWeekIndex(it)
                },
                weekIndexChangedEvent = {
                    mainViewModel.refreshWeekMap(it)
                },
                itemCalendarSelectedEvent = { itemSelected, weekOffset ->
                    mainViewModel.updateItemDayUIModelSelected(
                        itemSelected,
                        weekOffset
                    )
                },
                calendarClickEvent = {
                    showAsBottomSheet { closeBottomSheet ->
                        BottomCalendarSheet(
                            simpleDateModel = itemDayUIModelSelected.simpleDateModel,
                            onDoneEvent = { itemDayFromCalendar ->
                                itemDayFromCalendar?.let {
                                    val weekOffset = mainViewModel.todayCalendar
                                        .toSimpleDateModel()
                                        .getWeeksOffset(it.simpleDateModel)

                                    mainViewModel.updateDataFromExternalItem(it, weekOffset)

                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            itemDayUIModelSelected.simpleDateModel.dayOfWeekIndex
                                        )
                                    }

                                    coroutineScope.launch {
                                        if (weekOffset < 0) {
                                            listState.animateScrollToItem(Int.MAX_VALUE / 2 + weekOffset + 1)
                                        } else if (weekOffset > 0) {
                                            listState.animateScrollToItem(Int.MAX_VALUE / 2 + weekOffset - 1)
                                        } else {
                                            listState.animateScrollToItem(Int.MAX_VALUE / 2)
                                        }
                                    }
                                }
                                closeBottomSheet()
                            }
                        )
                    }
                }
            )
        }

    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MainUI(
    calendarMap: Map<Int, List<ItemDayUIModel>> = mapOf(),
    pagerState: PagerState = rememberPagerState(),
    listState: LazyListState = rememberLazyListState(Int.MAX_VALUE / 2),
    dayOfWeekIndexState: Int = 0,
    weekOffsetState: Int = 0,
    itemDayUIModelSelected: ItemDayUIModel = ItemDayUIModel(),
    updateWeekEvent: (Int) -> Unit = {},
    weekIndexChangedEvent: (Int) -> Unit = {},
    addButtonClickEvent: () -> Unit = {},
    calendarClickEvent: () -> Unit = {},
    itemCalendarSelectedEvent: (ItemDayUIModel, Int) -> Unit = { _, _ -> }
) {

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        val localCoroutine = rememberCoroutineScope()

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page ->
                updateWeekEvent(page)
            }
        }

        Column(Modifier.fillMaxSize()) {
            MenuRowComponent(
                onAddClicked = { addButtonClickEvent() },
                onCalendarClick = { calendarClickEvent() }
            )
            CalendarHeaderComponent(
                modifier = Modifier.fillMaxWidth(),
                listState = listState,
                calendarWeekMap = calendarMap,
                weekIndexChanged = {
                    weekIndexChangedEvent(it)
                },
                itemDayUIModelSelected = itemDayUIModelSelected,
                itemDayClickedEvent = { itemDayUIModelSelected, weekOffset ->
                    itemCalendarSelectedEvent(itemDayUIModelSelected, weekOffset)

                    localCoroutine.launch {
                        pagerState.animateScrollToPage(itemDayUIModelSelected.simpleDateModel.dayOfWeekIndex)
                    }
                })

            HorizontalPager(
                modifier = Modifier.weight(1f),
                count = daysInWeekArray.size,
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

@Preview
@Composable
fun BottomCalendarSheet(
    modifier: Modifier = Modifier,
    simpleDateModel: SimpleDateModel = Calendar.getInstance().toSimpleDateModel(),
    onCancelEvent: () -> Unit = {},
    onDoneEvent: (ItemDayUIModel?) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        var dateSelected = remember<ItemDayUIModel?> { null }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            Button(modifier = Modifier.align(Alignment.CenterStart), onClick = { /*TODO*/ }) {
                Text(text = "Cancel")
            }
            Text(modifier = Modifier.align(Alignment.Center), text = "Select Day")
            Button(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = { onDoneEvent(dateSelected) }) {
                Text(text = "Done")
            }
        }
        AndroidView(modifier = Modifier.fillMaxWidth(), factory = {
            CalendarView(it).apply {
                firstDayOfWeek = Calendar.MONDAY
            }
        }, update = {
            it.date = simpleDateModel.toCalendar().timeInMillis
            it.setOnDateChangeListener { _, year, month, day ->
                dateSelected = SimpleDateModel(dayOfMonth = day, month = month, year = year)
                    .toCalendar()
                    .toItemDayUIModel()
            }
        })
    }
}

@OptIn(ExperimentalSnapperApi::class)
@Composable
fun CalendarHeaderComponent(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(Int.MAX_VALUE / 2),
    calendarWeekMap: Map<Int, List<ItemDayUIModel>> = mutableMapOf(),
    itemDayUIModelSelected: ItemDayUIModel = ItemDayUIModel(),
    weekIndexChanged: (Int) -> Unit = {},
    itemDayClickedEvent: (ItemDayUIModel, Int) -> Unit = { _, _ -> }
) {
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
fun MenuRowComponent(
    modifier: Modifier = Modifier,
    onAddClicked: () -> Unit = {},
    onCalendarClick: () -> Unit = {}
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Icon(
            modifier = Modifier
                .padding(12.dp)
                .clickable { onAddClicked() },
            imageVector = Icons.Filled.Add,
            tint = MaterialTheme.colors.onBackground,
            contentDescription = "ADD"
        )
        Icon(
            modifier = Modifier
                .padding(12.dp)
                .clickable { onCalendarClick() },
            imageVector = Icons.Filled.CalendarToday,
            tint = MaterialTheme.colors.onBackground,
            contentDescription = "Calendar"
        )
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
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = itemDayUIModel.textDay
        )
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

fun SimpleDateModel.getWeeksOffset(item: SimpleDateModel): Int {
    val reference = this.toCalendar()
    val calendarDate = item.toCalendar()

    val differenceTimeMillis = reference.timeInMillis - calendarDate.timeInMillis
    val sign = -sign(differenceTimeMillis.toDouble()).toInt()

    val differenceCalendar = Calendar.getInstance().apply { // FIXME: This method generate errors for mondays
        timeInMillis = abs(differenceTimeMillis)
    }

    return sign * differenceCalendar.get(Calendar.WEEK_OF_YEAR)
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

