package com.homedepot.supplychain.enterpriselabormanagement.utils;

import com.homedepot.supplychain.enterpriselabormanagement.constants.ElmTransactionBqHeaders;
import com.homedepot.supplychain.enterpriselabormanagement.exceptions.DataProcessingException;
import com.homedepot.supplychain.enterpriselabormanagement.models.hdw.*;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
public final class TransactionUtils {
    private TransactionUtils() {
        //util class
    }
    public static void processTransaction(HdwTransaction hdwTransaction) {
        //data enrichment based on LocationUom, SkuNumber and BuildId
        hdwTransaction.getAttributes().put(ElmTransactionBqHeaders.PUBLISH_TIMESTAMP,
                CommonUtils.getTimeStampToBqFormat((String) hdwTransaction.getAttributes().get(ElmTransactionBqHeaders.PUBLISH_TIMESTAMP)));
        if(!ObjectUtils.isEmpty(hdwTransaction.getData().getTransactionTimestamp())) {
            hdwTransaction.getData().setTransactionTimestamp(
                    CommonUtils.getTimeStampToBqFormat(hdwTransaction.getData().getTransactionTimestamp()));
        }
        if(!ObjectUtils.isEmpty(hdwTransaction.getData().getLpns())) {
            hdwTransaction.getData().getLpns().forEach(
                    lpn -> lpn.getLocations().forEach(
                            location -> {
                                location.setTransactionTimeStamp(CommonUtils.getTimeStampToBqFormat(location.getTransactionTimeStamp()));
                                populateDimensions(location, hdwTransaction.getData().getBuilds());
                            }));
        }
    }

    private static void populateDimensions(Location location, List<Build> builds) {
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
                List<PackageHierarchy> packageHierarchies = build.getPackageHierarchies().stream()
                        .filter(packageHierarchy -> Objects.equals(packageHierarchy.getHierarchyLevelCode(), locationUom)).toList();
                if(packageHierarchies.isEmpty()) {
                    throw new DataProcessingException(String.format("No packageHierarchies found with level_code %s for build_id %s sku %s", locationUom, buildId, skuNumber));
                } else if(packageHierarchies.size()>1) {
                    throw new DataProcessingException(String.format("Multiple packageHierarchies found with level_code %s for build_id %s sku %s", locationUom, buildId, skuNumber));
                } else {
                    PackageHierarchy packageHierarchy = packageHierarchies.get(0);
                    sku.setLength(packageHierarchy.getDepth());
                    sku.setHeight(packageHierarchy.getHeight());
                    sku.setWidth(packageHierarchy.getWidth());
                    sku.setWeight(packageHierarchy.getWeight());
                    sku.setVolume(CommonUtils.calculateVolume(packageHierarchy.getDepth(),packageHierarchy.getWidth(),packageHierarchy.getHeight()));
                    sku.setWeightUom(packageHierarchy.getWeightUom());
                    sku.setSizeUom(packageHierarchy.getSizeUom());
                    sku.setPackageEachQty(packageHierarchy.getPackageEachQty());
                    sku.setPackageUnitQty(packageHierarchy.getPackageUnitQty());
                }
            }
        });
    }
}
