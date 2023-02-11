package com.yapp.buddycon.domain.usecase.giftcon

import androidx.paging.PagingData
import com.yapp.buddycon.domain.model.CouponInfo
import com.yapp.buddycon.domain.repository.CouponRepository
import com.yapp.buddycon.domain.repository.CouponType
import com.yapp.buddycon.domain.repository.SortMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCouponInfoUseCase @Inject constructor(
    private val couponRepository: CouponRepository
) {
    operator fun invoke(
        usable: Boolean,
        sort: SortMode,
        couponType: CouponType
    ): Flow<PagingData<CouponInfo>> =
        couponRepository.getCouponList(usable, sort, couponType)
}