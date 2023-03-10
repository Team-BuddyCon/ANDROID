package com.yapp.buddycon.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yapp.buddycon.domain.model.CouponItem
import com.yapp.buddycon.domain.model.CouponType
import com.yapp.buddycon.domain.model.SortMode
import com.yapp.buddycon.domain.model.TabMode
import com.yapp.buddycon.domain.usecase.coupon.GetCouponInfoUseCase
import com.yapp.buddycon.domain.usecase.login.GetBootInfoUseCase
import com.yapp.buddycon.domain.usecase.login.SaveBootInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class BuddyConViewModel @Inject constructor(
    private val getBootInfoUseCase: GetBootInfoUseCase,
    private val saveBootInfoUseCase: SaveBootInfoUseCase,
    private val getCouponInfoUseCase: GetCouponInfoUseCase
) : ViewModel() {

    private val _isDimState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDimState: StateFlow<Boolean> = _isDimState.asStateFlow()

    private val _isFabState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFabState: StateFlow<Boolean> = _isFabState.asStateFlow()

    private val _isTooltipState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isTooltipState: StateFlow<Boolean> = _isTooltipState.asStateFlow()

    private val _isBottomSheetState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isBottomSheetState: StateFlow<Boolean> = _isBottomSheetState.asStateFlow()

    private val _tabModeState: MutableStateFlow<TabMode> = MutableStateFlow(TabMode.Usable)
    val tabModeState = _tabModeState.asStateFlow()


    private val _sortModeState: MutableStateFlow<SortMode> = MutableStateFlow(SortMode.ExpireDate)
    val sortModeState = _sortModeState.asStateFlow()

    private val _couponTypeState: MutableStateFlow<CouponType> =
        MutableStateFlow(CouponType.GiftCon)
    val couponTypeState = _couponTypeState.asStateFlow()

    val couponPagingData: Flow<PagingData<CouponItem>> =
        combine(
            tabModeState,
            sortModeState,
            couponTypeState
        ) { tabMode, sortMode, couponType ->
            Triple(tabMode, sortMode, couponType)
        }.distinctUntilChanged()
            .flatMapLatest { requestCouponList() }
            .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            _isTooltipState.value = getBootInfoUseCase().first().not()
        }
    }

    private fun requestCouponList(): Flow<PagingData<CouponItem>> =
        getCouponInfoUseCase(
            usable = when (tabModeState.value) {
                TabMode.Usable -> true
                else -> false
            },
            sort = sortModeState.value,
            couponType = couponTypeState.value
        )

    fun saveBootInfo() {
        viewModelScope.launch {
            saveBootInfoUseCase()
        }
    }

    fun changeFab() {
        _isFabState.value = !_isFabState.value
        _isDimState.value = _isDimState.value.not()
        _isTooltipState.value = false
    }

    fun closeTooltip() {
        _isTooltipState.value = false
    }

    fun changeSortMode(sortMode: SortMode) {
        _sortModeState.value = sortMode
        _isBottomSheetState.value = false
        _isDimState.value = false
    }

    fun changeBottomSheet() {
        _isBottomSheetState.value = _isBottomSheetState.value.not()
        _isDimState.value = _isDimState.value.not()
    }

    fun hideBottomSheet() {
        _isBottomSheetState.value = false
        _isDimState.value = false
    }

    fun changeTabMode(tabMode: TabMode) {
        if(tabMode == TabMode.Made){
            changeCouponType(CouponType.Made)
            changeSortMode(SortMode.NoShared)
        }

        if(tabModeState.value == TabMode.Made){
            changeCouponType(CouponType.Custom)
            changeSortMode(SortMode.ExpireDate)
        }
        _tabModeState.value = tabMode
    }

    fun changeCouponType(couponType: CouponType){
        _couponTypeState.value = couponType
    }
}