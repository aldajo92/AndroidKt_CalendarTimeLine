package com.aldajo92.calendarswipeexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel : ViewModel() {

    private val _starDayWeekIndex = MutableStateFlow(0) // TODO: with zero starts from sunday, with one starts from monday and so on
    val starDayWeekIndex: StateFlow<Int> = _starDayWeekIndex

    val todayCalendar = Calendar.getInstance()
    private val todayItemDayUIModel = todayCalendar.toItemDayUIModel(_starDayWeekIndex.value)

    val calendarMap = mutableMapOf<Int, List<ItemDayUIModel>>()

    private val _itemDayUIModelSelectedFlow = MutableStateFlow(todayItemDayUIModel)
    val itemDayUIModelSelectedFlow: StateFlow<ItemDayUIModel> = _itemDayUIModelSelectedFlow

    private val _weekOffsetFlow = MutableStateFlow(0)
    val weekOffsetFlow: StateFlow<Int> = _weekOffsetFlow

    fun updateItemDayUIModelSelected(itemDayUIModel: ItemDayUIModel, weekOffset: Int) {
        _itemDayUIModelSelectedFlow.value = itemDayUIModel
        _weekOffsetFlow.value = weekOffset
    }

    fun updateByWeekIndex(dayOfWeekIndex: Int) {
        getItemDayUIModelFromIndex(dayOfWeekIndex, _weekOffsetFlow.value) {
            _itemDayUIModelSelectedFlow.value = it
        }
    }

    fun refreshWeekMap(weekIndex: Int) {
        _weekOffsetFlow.value = weekIndex

        calendarMap.saveToMapNoDuplicate(
            (weekIndex + 1) to todayCalendar.weekItemDaysFromWeeksOffset(
                weekIndex + 1,
                todayCalendar,
                dateComparator,
                _starDayWeekIndex.value
            )
        )
        calendarMap.saveToMapNoDuplicate(
            weekIndex to todayCalendar.weekItemDaysFromWeeksOffset(
                weekIndex, todayCalendar,
                dateComparator,
                _starDayWeekIndex.value
            )
        )
        calendarMap.saveToMapNoDuplicate(
            (weekIndex - 1) to todayCalendar.weekItemDaysFromWeeksOffset(
                weekIndex - 1,
                todayCalendar,
                dateComparator,
                _starDayWeekIndex.value
            )
        )
    }

    private fun getItemDayUIModelFromIndex(
        dayOfWeekIndexState: Int,
        weekIndexState: Int,
        resultCallback: (ItemDayUIModel) -> Unit = {}
    ) {
        val result = calendarMap[weekIndexState]?.get(dayOfWeekIndexState) ?: run {
            refreshWeekMap(weekIndexState)
            calendarMap[weekIndexState]?.get(dayOfWeekIndexState)!!
        }
        resultCallback(result)
    }

    fun updateDataFromExternalItem(externalItemDayUIModel: ItemDayUIModel, weekOffset: Int) {
        val dayOfWeek = externalItemDayUIModel.simpleDateModel.dayOfWeekIndex
        Log.i(
            "itemDayUI_weekOffset",
            weekOffset.toString()
        )
        // Here we use the item directly from map from the date created by an external source
        getItemDayUIModelFromIndex(dayOfWeek, weekOffset) {
            updateItemDayUIModelSelected(
                it,
                weekOffset
            )
        }
    }

}

class MainActivity : ComponentActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    @SuppressLint("CoroutineCreationDuringComposition")
    @OptIn(ExperimentalPagerApi::class, ExperimentalLifecycleComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val calendarMap = mainViewModel.calendarMap

            val listState = rememberLazyListState(Int.MAX_VALUE / 2)
            val weekOffsetState by mainViewModel.weekOffsetFlow.collectAsStateWithLifecycle()

            val itemDayUIModelSelected by mainViewModel.itemDayUIModelSelectedFlow.collectAsStateWithLifecycle()
            val pagerState = rememberPagerState(
                itemDayUIModelSelected.simpleDateModel.dayOfWeekIndex
            )

            val startDayWeekIndex by mainViewModel.starDayWeekIndex.collectAsStateWithLifecycle(1)

            val coroutineScope = rememberCoroutineScope()

            MainUI(
                calendarMap,
                pagerState,
                listState,
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
                            startDayWeekIndex = startDayWeekIndex,
                            simpleDateModel = itemDayUIModelSelected.simpleDateModel,
                            onDoneEvent = { itemDayFromCalendar ->
                                itemDayFromCalendar?.let {
                                    val weekOffset = mainViewModel.todayCalendar
                                        .toSimpleDateModel(startDayWeekIndex)
                                        .getWeeksOffset(it.simpleDateModel, startDayWeekIndex)

                                    mainViewModel.updateDataFromExternalItem(it, weekOffset)

                                    coroutineScope.launch {
                                        delay(200)
                                        pagerState.scrollToPage(
                                            itemDayUIModelSelected.simpleDateModel.dayOfWeekIndex
                                        )
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

        LaunchedEffect(key1 = weekOffsetState, block = {
            listState.scrollToItem(Int.MAX_VALUE / 2 + weekOffsetState)
        })

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
                        pagerState.scrollToPage(itemDayUIModelSelected.simpleDateModel.dayOfWeekIndex)
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
            val dateModel = itemDayUIModelSelected.simpleDateModel
            Text(
                modifier = Modifier.padding(horizontal = 10.dp),
                text = "dayOfWeek: ${dateModel.dayOfWeekIndex} | date: ${dateModel.dayOfMonth}/${dateModel.month}/${dateModel.year}"
            )
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "weekIndex: $weekOffsetState")
                Text(text = "dayOfWeekIndex: ${itemDayUIModelSelected.simpleDateModel.dayOfWeekIndex}")
            }
        }
    }
}

@Preview
@Composable
fun BottomCalendarSheet(
    modifier: Modifier = Modifier,
    startDayWeekIndex: Int = 0,
    simpleDateModel: SimpleDateModel = Calendar.getInstance().toSimpleDateModel(startDayWeekIndex),
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
                firstDayOfWeek = Calendar.SUNDAY + startDayWeekIndex
            }
        }, update = {
            it.date = simpleDateModel.toCalendar().timeInMillis
            it.setOnDateChangeListener { _, year, month, day ->
                dateSelected = SimpleDateModel(dayOfMonth = day, month = month, year = year)
                    .toCalendar()
                    .toItemDayUIModel(startDayWeekIndex)
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
            textDay = getDaysInWeekArray(it)
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
fun Calendar.getDayOfWeekIndex(
    startDayWeekIndex: Int = 0
): Int {
    val dayOfWeekFromCalendar = this.get(Calendar.DAY_OF_WEEK) + 7
    return (dayOfWeekFromCalendar - startDayWeekIndex - 1) % 7
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
    comparativeDay: (Calendar, Calendar) -> Boolean = { _, _ -> false },
    startDayWeekIndex: Int = 0
): List<ItemDayUIModel> {
    val localCalendar = this.clone() as Calendar
    localCalendar.add(Calendar.DATE, 7 * index)

    val dayOfWeek = localCalendar.getDayOfWeekIndex(startDayWeekIndex)

    val calendarNearMonday = localCalendar.clone() as Calendar
    calendarNearMonday.add(Calendar.DATE, -dayOfWeek)

    return (0..6).map {
        val tmpCalendar = calendarNearMonday.clone() as Calendar
        tmpCalendar.add(Calendar.DATE, it)
        val dayNumberInMonth = tmpCalendar.get(Calendar.DAY_OF_MONTH)
        ItemDayUIModel(
            simpleDateModel = tmpCalendar.toSimpleDateModel(startDayWeekIndex),
            numberDay = "$dayNumberInMonth",
            textDay = getDaysInWeekArray(it, startDayWeekIndex),
            markDate = comparativeDay(currentCalendar, tmpCalendar)
        )
    }
}

fun Calendar.toSimpleDateModel(
    startDayWeekIndex: Int = 0
) = SimpleDateModel(
    dayOfWeekIndex = this.getDayOfWeekIndex(startDayWeekIndex),
    dayOfMonth = this.getDayOfMonth(),
    month = this.getMonth(),
    year = this.getYear()
)

fun Calendar.toItemDayUIModel(
    startDayWeekIndex: Int = 0
) = this.toSimpleDateModel(startDayWeekIndex).let {
    ItemDayUIModel(
        simpleDateModel = it,
        numberDay = it.dayOfMonth.toString(),
        textDay = getDaysInWeekArray(it.dayOfWeekIndex, startDayWeekIndex)
    )
}

fun SimpleDateModel.getWeeksOffset(
    item: SimpleDateModel,
    startDayWeekIndex: Int = 0
): Int {
    val reference = this.toCalendar().apply {
        val dayOfWeek = this.getDayOfWeekIndex(startDayWeekIndex)
        this.add(Calendar.DATE, -dayOfWeek)
    }

    val calendarDate = item.toCalendar().apply {
        val dayOfWeek = this.getDayOfWeekIndex(startDayWeekIndex)
        this.add(Calendar.DATE, -dayOfWeek)
    }

    val differenceTimeMillis =
        (calendarDate.timeInMillis / 1000L) - (reference.timeInMillis / 1000L)

    // 1 day = (60s / 1min) * (60 min / 1hour) * (24 hour/ 1day) * (7day)  = 604800L
    return (differenceTimeMillis / 604800L).toInt()
}

fun ItemDayUIModel.equalsInSimpleDate(itemDayUIModel: ItemDayUIModel) =
    this.simpleDateModel == itemDayUIModel.simpleDateModel

fun SimpleDateModel.toCalendar(): Calendar = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, this@toCalendar.dayOfMonth)
    set(Calendar.MONTH, this@toCalendar.month)
    set(Calendar.YEAR, this@toCalendar.year)
}

////////////////////////////////////////////////////////////////////////////

val daysInWeekArray = arrayOf("S", "M", "T", "W", "T", "F", "S")

fun getDaysInWeekArray(position: Int, startDayWeekIndex: Int = 0) =
    daysInWeekArray[(position + startDayWeekIndex) % 7]

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
