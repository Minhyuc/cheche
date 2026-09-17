package com.cheche.facility.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KspoFacilityItem(
        @JsonProperty("faci_cd") String facilityCode,
        @JsonProperty("faci_nm") String name,
        @JsonProperty("faci_gb_nm") String classification,
        @JsonProperty("fcob_nm") String businessType,
        @JsonProperty("ftype_nm") String facilityType,
        @JsonProperty("faci_stat_nm") String status,
        @JsonProperty("faci_road_addr") String roadAddress,
        @JsonProperty("faci_road_daddr") String roadAddressDetail,
        @JsonProperty("faci_addr") String address,
        @JsonProperty("faci_daddr") String addressDetail,
        @JsonProperty("faci_tel_no") String phone,
        @JsonProperty("faci_mng_user_telno") String managerPhone,
        @JsonProperty("faci_homepage") String homepage,
        @JsonProperty("cp_nm") String province,
        @JsonProperty("cpb_nm") String district
) {}
