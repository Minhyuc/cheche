package com.cheche.facility.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PublicOpenFacilityItem(
        @JsonProperty("OPEN_FCLTY_NM") String name,
        @JsonProperty("OPEN_LC_NM") String locationName,
        @JsonProperty("OPEN_FCLTY_TYPE") String facilityType,
        @JsonProperty("RSTDE") String closedDays,
        @JsonProperty("WEEKDAY_OPER_OPEN_HHMM") String weekdayOpeningTime,
        @JsonProperty("WEEKDAY_OPER_COLSE_HHMM") String weekdayClosingTime,
        @JsonProperty("WKEND_OPER_OPEN_HHMM") String weekendOpeningTime,
        @JsonProperty("WKEND_OPER_CLOSE_HHMM") String weekendClosingTime,
        @JsonProperty("PCHRG_USE_YN") String paid,
        @JsonProperty("RNTFEE") String fee,
        @JsonProperty("ACEPTNC_POSBL_CO") String capacity,
        @JsonProperty("ETC_FCLTY") String amenities,
        @JsonProperty("SBSCRPTN_MTH_SE") String applicationMethod,
        @JsonProperty("FCLTY_PIC_INFO") String imageUrl,
        @JsonProperty("RDNMADR") String roadAddress,
        @JsonProperty("LNMADR") String lotAddress,
        @JsonProperty("INSTITUTION_NM") String institutionName,
        @JsonProperty("PHONE_NUMBER") String phone,
        @JsonProperty("HOMEPAGE_URL") String homepageUrl,
        @JsonProperty("LATITUDE") String latitude,
        @JsonProperty("LONGITUDE") String longitude
) {}
