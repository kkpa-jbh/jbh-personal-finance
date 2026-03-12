package com.jbh.finance.application.feature.movement.services;

import com.jbh.finance.domain.product.vo.ProductPK;

record ProcessMovementOptions(ProductPK productPK, boolean skipMovementPersistence) {}
