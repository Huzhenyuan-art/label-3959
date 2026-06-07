package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.OperationLog;
import com.example.demo.common.Result;
import com.example.demo.dto.CategoryStatsDTO;
import com.example.demo.entity.Product;
import com.example.demo.enums.OperationTypeEnum;
import com.example.demo.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品接口
 * 演示：分页查询、多条件过滤、自定义 SQL 统计
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 分页查询（演示分页插件 + 条件构造器） */
    @GetMapping("/page")
    public Result<IPage<Product>> page(@RequestParam(defaultValue = "1") int current,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) BigDecimal minPrice,
                                       @RequestParam(required = false) BigDecimal maxPrice,
                                       @RequestParam(required = false) Integer minStock,
                                       @RequestParam(required = false) Integer maxStock) {
        return Result.ok(productService.pageProducts(current, size, name, category, minPrice, maxPrice, minStock, maxStock));
    }

    /** 全量列表（无分页） */
    @GetMapping
    public Result<List<Product>> list() {
        return Result.ok(productService.list());
    }

    /** 分类统计（演示自定义 @Select 注解 SQL） */
    @GetMapping("/stats")
    public Result<List<CategoryStatsDTO>> stats(@RequestParam(required = false) String name,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) BigDecimal minPrice,
                                                @RequestParam(required = false) BigDecimal maxPrice,
                                                @RequestParam(required = false) Integer minStock,
                                                @RequestParam(required = false) Integer maxStock) {
        return Result.ok(productService.getCategoryStats(name, category, minPrice, maxPrice, minStock, maxStock));
    }

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) return Result.fail(404, "商品不存在");
        return Result.ok(product);
    }

    @PostMapping
    @OperationLog(type = OperationTypeEnum.PRODUCT_CREATE, targetType = "product", targetIdExpression = "#result.data.id")
    public Result<Product> create(@RequestBody Product product) {
        return Result.ok(productService.createProduct(product));
    }

    @PutMapping("/{id}")
    @OperationLog(type = OperationTypeEnum.PRODUCT_UPDATE, targetType = "product", targetIdExpression = "#id")
    public Result<Product> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        return Result.ok(productService.updateProduct(product));
    }

    @DeleteMapping("/{id}")
    @OperationLog(type = OperationTypeEnum.PRODUCT_DELETE, targetType = "product", targetIdExpression = "#id")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.ok();
    }
}
