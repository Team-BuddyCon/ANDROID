package com.yapp.buddycon.domain.repository

import com.yapp.buddycon.domain.model.AddCouponResult
import com.yapp.buddycon.domain.model.CouponInputInfo
import kotlinx.coroutines.flow.Flow

interface AddCouponRepository {
    fun getGifticionInfo(barcodeNumber: String): Flow<CouponInputInfo>
    fun getCustomCouponInfo(barcodeNumber: String): Flow<CouponInputInfo>
    fun addGifticon(imageUriPath: String, couponInputInfo: CouponInputInfo): Flow<AddCouponResult>
    fun addCustomCoupon(imageUriPath: String, couponInputInfo: CouponInputInfo): Flow<AddCouponResult>
}