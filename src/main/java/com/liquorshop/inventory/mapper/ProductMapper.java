package com.liquorshop.inventory.mapper;

import com.liquorshop.inventory.dto.ProductResponse;
import com.liquorshop.inventory.entity.ProductEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(ProductEntity entity);
}
