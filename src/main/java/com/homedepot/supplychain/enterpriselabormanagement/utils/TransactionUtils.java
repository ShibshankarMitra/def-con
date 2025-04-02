package com.homedepot.supplychain.enterpriselabormanagement.utils;

import com.homedepot.supplychain.enterpriselabormanagement.constants.CommonConstants;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.DataProcessingException;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
@Slf4j
public final class TransactionUtils {
    public static final String PACKAGE_HIERARCHY_SEQUENCE = "PL,CA,PK,EA";
    public static final String PICKING_TRANSACTIONS = "PICK_TASKING_LM,PICK_LPN_FROM_ACTIVE_LM,PICK_LPN_FROM_RESERVE_LM";
    private TransactionUtils() {
        //util class
    }
    public static void processTransaction(HdwTransaction hdwTransaction) {
        //data enrichment based on LocationUom, SkuNumber and BuildId
        hdwTransaction.getLaborEvent().setPublishTimestamp(
                CommonUtils.getTimeStampToBqFormat(hdwTransaction.getLaborEvent().getPublishTimestamp()));
        if(!ObjectUtils.isEmpty(hdwTransaction.getLaborEventDetail().getTransactionTimestamp())) {
            hdwTransaction.getLaborEventDetail().setTransactionTimestamp(
                    CommonUtils.getTimeStampToBqFormat(hdwTransaction.getLaborEventDetail().getTransactionTimestamp()));
        }
        if(!ObjectUtils.isEmpty(hdwTransaction.getLaborEventDetail().getLpns())) {
            hdwTransaction.getLaborEventDetail().getLpns().forEach(
                    lpn -> lpn.getLocations().forEach(
                            location -> {
                                location.setTransactionTimeStamp(CommonUtils.getTimeStampToBqFormat(location.getTransactionTimeStamp()));
                                populateDimensions(location, hdwTransaction.getLaborEventDetail().getBuilds(), hdwTransaction.getLaborEvent().getTraceId(), hdwTransaction.getLaborEventDetail().getTransactionId(), hdwTransaction.getLaborEvent().getActivity());
                            }));
        }
    }

    private static void populateDimensions(Location location, List<Build> builds, String traceId, String transactionId, String activity) {
        location.getSkus().forEach(sku -> {
            String locationUom= location.getLocationUom();
            String buildId=sku.getBuildId();
            String skuNumber=sku.getSkuNumber();
            List<Build> filteredBuilds = builds.stream().filter(build -> (Objects.equals(build.getBuildId(), buildId) && Objects.equals(build.getSku(), skuNumber))).toList();
            if(filteredBuilds.isEmpty()) {
                throw new DataProcessingException(String.format("No builds found with build_id %s sku %s", buildId, skuNumber));
            } else if(filteredBuilds.size()>1) {
                throw new DataProcessingException(String.format("Multiple builds found with build_id %s sku %s", buildId, skuNumber));
            } else{
                Build build = filteredBuilds.get(0);
                sku.setIsMx(build.getIsMx());
                populateSkuWithPackageQuantity(sku, build, traceId, transactionId);
                List<PackageHierarchy> packageHierarchies = build.getPackageHierarchies().stream()
                        .filter(packageHierarchy -> Objects.equals(packageHierarchy.getHierarchyLevelCode(), locationUom)).toList();
                if(packageHierarchies.isEmpty()) {
                    if(!Arrays.asList(PICKING_TRANSACTIONS.split(CommonConstants.COMMA_SEPARATOR)).contains(activity)){
                        // if not action from the given list then check for the divisibility logic else throw exception.
                        populateSkuWithPackageHierarchy(sku, build.getPackageHierarchies(), traceId, transactionId, locationUom);
                    }else{
                        throw new DataProcessingException(String.format("No packageHierarchies found with level_code %s for build_id %s sku %s, traceId: %s, transactionId: %s", locationUom, buildId, skuNumber, traceId, transactionId));
                    }
                } else if(packageHierarchies.size()>1) {
                    throw new DataProcessingException(String.format("Multiple packageHierarchies found with level_code %s for build_id %s sku %s, traceId: %s, transactionId: %s", locationUom, buildId, skuNumber, traceId, transactionId));
                } else {
                    PackageHierarchy packageHierarchy = packageHierarchies.get(0);
                    populateSkuFields(sku, packageHierarchy, locationUom);
                }
            }
        });
    }

    private static void populateSkuWithPackageQuantity(Sku sku, Build build, String traceId, String transactionId) {
        build.getPackageHierarchies().forEach(packageHierarchy -> {
            switch (packageHierarchy.getHierarchyLevelCode()) {
                case "EA" -> sku.setPackageEachQuantity(packageHierarchy.getPackageEachQty());
                case "PL" -> sku.setPackagePalletQuantity(packageHierarchy.getPackageEachQty());
                case "CA" -> sku.setPackageCaseQuantity(packageHierarchy.getPackageEachQty());
                case "PK" -> sku.setPackagePackQuantity(packageHierarchy.getPackageEachQty());
                default ->
                        log.warn("Missing or unexpected value of hierarchyLevelCode: {}, traceId: {}, transactionId: {}", packageHierarchy.getHierarchyLevelCode(), traceId, transactionId);
            }
        });
    }

    private static void populateSkuWithPackageHierarchy(Sku sku, List<PackageHierarchy> packageHierarchies, String traceId, String transactionId, String locationUom) {
        Map<String, PackageHierarchy> packageHierarchyMap = packageHierarchies.stream()
                .collect(Collectors.toMap(PackageHierarchy::getHierarchyLevelCode, packageHierarchy -> packageHierarchy));
        Optional<PackageHierarchy> packageHierarchyMatch = Arrays.stream(PACKAGE_HIERARCHY_SEQUENCE.split(CommonConstants.COMMA_SEPARATOR))
                .map(packageHierarchyMap::get)
                .filter(Objects::nonNull)
                .filter(packageHierarchy -> {
                    BigDecimal remainder = sku.getQuantity().remainder(packageHierarchy.getPackageEachQty());
                    return remainder.equals(BigDecimal.valueOf(0.0)) || remainder.equals(BigDecimal.valueOf(0));
                })
                .findFirst();
        if(packageHierarchyMatch.isPresent()){
            sku.setPackageUnitQty(packageHierarchyMatch.get().getPackageEachQty());
            sku.setSkuBuildUom(packageHierarchyMatch.get().getHierarchyLevelCode());
            populateSkuFields(sku, packageHierarchyMatch.get(), locationUom);
        }else{
            throw new DataProcessingException(String.format("No packageHierarchies found with level_code %s for build_id %s sku %s, traceId: %s, transactionId: %s", locationUom, sku.getBuildId(), sku.getSkuNumber(), traceId, transactionId));
        }
    }

    private static void populateSkuFields(Sku sku, PackageHierarchy packageHierarchy, String locationUom) {
        sku.setLength(roundBigDecimalValue(packageHierarchy.getDepth()));
        sku.setHeight(roundBigDecimalValue(packageHierarchy.getHeight()));
        sku.setWidth(roundBigDecimalValue(packageHierarchy.getWidth()));
        sku.setWeight(roundBigDecimalValue(packageHierarchy.getWeight()));
        sku.setVolume(roundBigDecimalValue(CommonUtils.calculateVolume(packageHierarchy.getDepth(),packageHierarchy.getWidth(),packageHierarchy.getHeight())));
        sku.setWeightUom(packageHierarchy.getWeightUom());
        sku.setSizeUom(packageHierarchy.getSizeUom());
        sku.setPackageUnitQty(ObjectUtils.isEmpty(sku.getPackageUnitQty())?packageHierarchy.getPackageUnitQty():sku.getPackageUnitQty());
        sku.setSkuBuildUom(ObjectUtils.isEmpty(sku.getSkuBuildUom())?locationUom:packageHierarchy.getHierarchyLevelCode());
    }

    public static double calculateTotalWorkedHours(String lastPunchLocalTime, String transactionLocalTimestamp) {
        return formatCicoTotalHoursWorked(Duration.between(LocalDateTime.parse(lastPunchLocalTime), LocalDateTime.parse(transactionLocalTimestamp)).toMinutes() / CommonConstants.ONE_HOUR_IN_MINUTES);
    }

    public static double formatCicoTotalHoursWorked(double totalWorkedHours){
        DecimalFormat decimalFormat = new DecimalFormat(CommonConstants.CICO_TOTAL_HOURS_PATTERN);
        return Double.parseDouble(decimalFormat.format(totalWorkedHours));
    }

    private static BigDecimal roundBigDecimalValue(BigDecimal value){
        return value.setScale(CommonConstants.ROUNDING_DIGIT, RoundingMode.HALF_DOWN);
    }
}
