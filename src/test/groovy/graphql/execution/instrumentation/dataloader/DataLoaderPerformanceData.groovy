package graphql.execution.instrumentation.dataloader

import graphql.Directives
import graphql.GraphQL
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.pubsub.CapturingSubscriber
import graphql.incremental.DelayedIncrementalPartialResult
import graphql.incremental.IncrementalExecutionResult
import graphql.schema.GraphQLSchema
import org.awaitility.Awaitility
import org.dataloader.DataLoaderRegistry
import org.reactivestreams.Publisher

class DataLoaderPerformanceData {

    private final BatchCompareDataFetchers batchCompareDataFetchers;

    DataLoaderPerformanceData(BatchCompareDataFetchers batchCompareDataFetchers) {
        this.batchCompareDataFetchers = batchCompareDataFetchers;
    }

    DataLoaderRegistry setupDataLoaderRegistry() {
        DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry()
        dataLoaderRegistry.register("departments", batchCompareDataFetchers.departmentsForShopDataLoader)
        dataLoaderRegistry.register("products", batchCompareDataFetchers.productsForDepartmentDataLoader)
    }

    GraphQL setupGraphQL(Instrumentation instrumentation) {
        GraphQLSchema schema = new BatchCompare().buildDataLoaderSchema(batchCompareDataFetchers)
        schema = schema.transform({ bldr -> bldr.additionalDirective(Directives.DeferDirective) })

        GraphQL.newGraphQL(schema)
                .instrumentation(instrumentation)
                .build()
    }

    GraphQL setupGraphQL() {
        GraphQLSchema schema = new BatchCompare().buildDataLoaderSchema(batchCompareDataFetchers)
        schema = schema.transform({ bldr -> bldr.additionalDirective(Directives.DeferDirective) })

        GraphQL.newGraphQL(schema)
                .build()
    }

    static def expectedData = [
            shops: [
                    [id         : "shop-1", name: "Shop 1",
                     departments: [[id: "department-1", name: "Department 1", products: [[id: "product-1", name: "Product 1"]]],
                                   [id: "department-2", name: "Department 2", products: [[id: "product-2", name: "Product 2"]]],
                                   [id: "department-3", name: "Department 3", products: [[id: "product-3", name: "Product 3"]]]
                     ]],
                    [id         : "shop-2", name: "Shop 2",
                     departments: [[id: "department-4", name: "Department 4", products: [[id: "product-4", name: "Product 4"]]],
                                   [id: "department-5", name: "Department 5", products: [[id: "product-5", name: "Product 5"]]],
                                   [id: "department-6", name: "Department 6", products: [[id: "product-6", name: "Product 6"]]]
                     ]],
                    [id         : "shop-3", name: "Shop 3",
                     departments: [[id: "department-7", name: "Department 7", products: [[id: "product-7", name: "Product 7"]]],
                                   [id: "department-8", name: "Department 8", products: [[id: "product-8", name: "Product 8"]]],
                                   [id: "department-9", name: "Department 9", products: [[id: "product-9", name: "Product 9"]]]]
                    ]]
    ]

    static def query = """
            query { 
                shops { 
                    id name 
                    departments { 
                        id name 
                        products { 
                            id name 
                        } 
                    } 
                } 
            }
            """

    static def expectedExpensiveData = [
            shops         : [[name                : "Shop 1",
                              departments         : [[name: "Department 1", products: [[name: "Product 1"]], expensiveProducts: [[name: "Product 1"]]],
                                                     [name: "Department 2", products: [[name: "Product 2"]], expensiveProducts: [[name: "Product 2"]]],
                                                     [name: "Department 3", products: [[name: "Product 3"]], expensiveProducts: [[name: "Product 3"]]]],
                              expensiveDepartments: [[name: "Department 1", products: [[name: "Product 1"]], expensiveProducts: [[name: "Product 1"]]],
                                                     [name: "Department 2", products: [[name: "Product 2"]], expensiveProducts: [[name: "Product 2"]]],
                                                     [name: "Department 3", products: [[name: "Product 3"]], expensiveProducts: [[name: "Product 3"]]]]],
                             [name                : "Shop 2",
                              departments         : [[name: "Department 4", products: [[name: "Product 4"]], expensiveProducts: [[name: "Product 4"]]],
                                                     [name: "Department 5", products: [[name: "Product 5"]], expensiveProducts: [[name: "Product 5"]]],
                                                     [name: "Department 6", products: [[name: "Product 6"]], expensiveProducts: [[name: "Product 6"]]]],
                              expensiveDepartments: [[name: "Department 4", products: [[name: "Product 4"]], expensiveProducts: [[name: "Product 4"]]],
                                                     [name: "Department 5", products: [[name: "Product 5"]], expensiveProducts: [[name: "Product 5"]]],
                                                     [name: "Department 6", products: [[name: "Product 6"]], expensiveProducts: [[name: "Product 6"]]]]],
                             [name                : "Shop 3",
                              departments         : [[name: "Department 7", products: [[name: "Product 7"]], expensiveProducts: [[name: "Product 7"]]],
                                                     [name: "Department 8", products: [[name: "Product 8"]], expensiveProducts: [[name: "Product 8"]]],
                                                     [name: "Department 9", products: [[name: "Product 9"]], expensiveProducts: [[name: "Product 9"]]]],
                              expensiveDepartments: [[name: "Department 7", products: [[name: "Product 7"]], expensiveProducts: [[name: "Product 7"]]],
                                                     [name: "Department 8", products: [[name: "Product 8"]], expensiveProducts: [[name: "Product 8"]]],
                                                     [name: "Department 9", products: [[name: "Product 9"]], expensiveProducts: [[name: "Product 9"]]]]]],
            expensiveShops: [[name                : "ExShop 1",
                              departments         : [[name: "Department 1", products: [[name: "Product 1"]], expensiveProducts: [[name: "Product 1"]]],
                                                     [name: "Department 2", products: [[name: "Product 2"]], expensiveProducts: [[name: "Product 2"]]],
                                                     [name: "Department 3", products: [[name: "Product 3"]], expensiveProducts: [[name: "Product 3"]]]],
                              expensiveDepartments: [[name: "Department 1", products: [[name: "Product 1"]], expensiveProducts: [[name: "Product 1"]]],
                                                     [name: "Department 2", products: [[name: "Product 2"]], expensiveProducts: [[name: "Product 2"]]],
                                                     [name: "Department 3", products: [[name: "Product 3"]], expensiveProducts: [[name: "Product 3"]]]]],
                             [name                : "ExShop 2",
                              departments         : [[name: "Department 4", products: [[name: "Product 4"]], expensiveProducts: [[name: "Product 4"]]],
                                                     [name: "Department 5", products: [[name: "Product 5"]], expensiveProducts: [[name: "Product 5"]]],
                                                     [name: "Department 6", products: [[name: "Product 6"]], expensiveProducts: [[name: "Product 6"]]]],
                              expensiveDepartments: [[name: "Department 4", products: [[name: "Product 4"]], expensiveProducts: [[name: "Product 4"]]],
                                                     [name: "Department 5", products: [[name: "Product 5"]], expensiveProducts: [[name: "Product 5"]]],
                                                     [name: "Department 6", products: [[name: "Product 6"]], expensiveProducts: [[name: "Product 6"]]]]],
                             [name                : "ExShop 3",
                              departments         : [[name: "Department 7", products: [[name: "Product 7"]], expensiveProducts: [[name: "Product 7"]]],
                                                     [name: "Department 8", products: [[name: "Product 8"]], expensiveProducts: [[name: "Product 8"]]],
                                                     [name: "Department 9", products: [[name: "Product 9"]], expensiveProducts: [[name: "Product 9"]]]],
                              expensiveDepartments: [[name: "Department 7", products: [[name: "Product 7"]], expensiveProducts: [[name: "Product 7"]]],
                                                     [name: "Department 8", products: [[name: "Product 8"]], expensiveProducts: [[name: "Product 8"]]],
                                                     [name: "Department 9", products: [[name: "Product 9"]], expensiveProducts: [[name: "Product 9"]]]]]]

    ]

    static void assertIncrementalExpensiveData(List<Map<String, Object>> incrementalResults) {
        // Ordering is non-deterministic, so we assert on the things we know are going to be true.

        assert incrementalResults.size() == 25
        // only the last payload has "hasNext=true"
        assert incrementalResults.subList(0, 24).every { it.hasNext == true }
        assert incrementalResults[24].hasNext == false

        // every payload has only 1 incremental item, and the data is the same for all of them
        assert incrementalResults.every { it.incremental.size() == 1 }

        def incrementalResultsItems = incrementalResults.collect { it.incremental[0] }

        // the order of the actual data is non-deterministic. So we assert via "any" that the data is there
        assert incrementalResultsItems.any { it == [path: ["shops", 0], data: [departments: [[name: "Department 1"], [name: "Department 2"], [name: "Department 3"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 0], data: [expensiveDepartments: [[name: "Department 1", products: [[name: "Product 1"]], expensiveProducts: [[name: "Product 1"]]], [name: "Department 2", products: [[name: "Product 2"]], expensiveProducts: [[name: "Product 2"]]], [name: "Department 3", products: [[name: "Product 3"]], expensiveProducts: [[name: "Product 3"]]]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 1], data: [expensiveDepartments: [[name: "Department 4", products: [[name: "Product 4"]], expensiveProducts: [[name: "Product 4"]]], [name: "Department 5", products: [[name: "Product 5"]], expensiveProducts: [[name: "Product 5"]]], [name: "Department 6", products: [[name: "Product 6"]], expensiveProducts: [[name: "Product 6"]]]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 1], data: [departments: [[name: "Department 4"], [name: "Department 5"], [name: "Department 6"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 2], data: [departments: [[name: "Department 7"], [name: "Department 8"], [name: "Department 9"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 2], data: [expensiveDepartments: [[name: "Department 7", products: [[name: "Product 7"]], expensiveProducts: [[name: "Product 7"]]], [name: "Department 8", products: [[name: "Product 8"]], expensiveProducts: [[name: "Product 8"]]], [name: "Department 9", products: [[name: "Product 9"]], expensiveProducts: [[name: "Product 9"]]]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 0, "departments", 0], data: [products: [[name: "Product 1"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 0, "departments", 0], data: [expensiveProducts: [[name: "Product 1"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 0, "departments", 1], data: [products: [[name: "Product 2"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 0, "departments", 1], data: [expensiveProducts: [[name: "Product 2"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 0, "departments", 2], data: [products: [[name: "Product 3"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 0, "departments", 2], data: [expensiveProducts: [[name: "Product 3"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 1, "departments", 0], data: [expensiveProducts: [[name: "Product 4"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 1, "departments", 0], data: [products: [[name: "Product 4"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 1, "departments", 1], data: [products: [[name: "Product 5"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 1, "departments", 1], data: [expensiveProducts: [[name: "Product 5"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 1, "departments", 2], data: [expensiveProducts: [[name: "Product 6"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 1, "departments", 2], data: [products: [[name: "Product 6"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 2, "departments", 0], data: [products: [[name: "Product 7"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 2, "departments", 0], data: [expensiveProducts: [[name: "Product 7"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 2, "departments", 1], data: [products: [[name: "Product 8"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 2, "departments", 1], data: [expensiveProducts: [[name: "Product 8"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 2, "departments", 2], data: [products: [[name: "Product 9"]]]] }
        assert incrementalResultsItems.any { it == [path: ["shops", 2, "departments", 2], data: [expensiveProducts: [[name: "Product 9"]]]] }
        assert incrementalResultsItems.any { it == [path: [], data: [expensiveShops: [[id: "exshop-1", name: "ExShop 1"], [id: "exshop-2", name: "ExShop 2"], [id: "exshop-3", name: "ExShop 3"]]]] }
    }

    static def expensiveQuery = """
            query { 
                shops { 
                    name 
                    departments { 
                        name 
                        products { 
                            name 
                        } 
                        expensiveProducts { 
                            name 
                        } 
                    } 
                    expensiveDepartments { 
                        name 
                        products { 
                            name 
                        } 
                        expensiveProducts { 
                            name 
                        } 
                    } 
                } 
                expensiveShops { 
                    name 
                    departments { 
                        name 
                        products { 
                            name 
                        } 
                        expensiveProducts { 
                            name 
                        } 
                    } 
                    expensiveDepartments { 
                        name 
                        products { 
                            name 
                        } 
                        expensiveProducts { 
                            name 
                        } 
                    } 
                } 
            }
            """

    static def expectedInitialDeferredData = [
            data   : [
                    shops: [
                            [id: "shop-1", name: "Shop 1"],
                            [id: "shop-2", name: "Shop 2"],
                            [id: "shop-3", name: "Shop 3"],
                    ]
            ],
            hasNext: true
    ]

    static def expectedListOfDeferredData = [
            [
                    hasNext    : true,
                    incremental: [[
                                          path: ["shops", 0],
                                          data: [
                                                  departments: [
                                                          [id: "department-1", name: "Department 1", products: [[id: "product-1", name: "Product 1"]]],
                                                          [id: "department-2", name: "Department 2", products: [[id: "product-2", name: "Product 2"]]],
                                                          [id: "department-3", name: "Department 3", products: [[id: "product-3", name: "Product 3"]]]
                                                  ]
                                          ]
                                  ]],
            ],
            [
                    hasNext    : true,
                    incremental: [[
                                          path: ["shops", 1],
                                          data: [
                                                  departments: [
                                                          [id: "department-4", name: "Department 4", products: [[id: "product-4", name: "Product 4"]]],
                                                          [id: "department-5", name: "Department 5", products: [[id: "product-5", name: "Product 5"]]],
                                                          [id: "department-6", name: "Department 6", products: [[id: "product-6", name: "Product 6"]]]
                                                  ]
                                          ],
                                  ]],
            ],
            [
                    hasNext    : false,
                    incremental: [[
                                          path: ["shops", 2],
                                          data: [
                                                  departments: [
                                                          [id: "department-7", name: "Department 7", products: [[id: "product-7", name: "Product 7"]]],
                                                          [id: "department-8", name: "Department 8", products: [[id: "product-8", name: "Product 8"]]],
                                                          [id: "department-9", name: "Department 9", products: [[id: "product-9", name: "Product 9"]]]
                                                  ]
                                          ]
                                  ]],
            ]
    ]


    static def deferredQuery = """
            query { 
                shops { 
                    id name 
                    ... @defer {
                        departments { 
                            id name 
                            products { 
                                id name 
                            } 
                        } 
                    }
                } 
            }
            """

    static def expensiveDeferredQuery = """
            query { 
                shops { 
                    id name 
                    ... @defer {
                        departments { 
                            name 
                            ... @defer {
                                products { 
                                    name 
                                } 
                            }
                            ... @defer {
                                expensiveProducts { 
                                    name 
                                } 
                            }
                        } 
                    }
                    ... @defer {
                        expensiveDepartments { 
                            name 
                            products { 
                                name 
                            } 
                            expensiveProducts { 
                                name 
                            } 
                        } 
                    }
                } 
                ... @defer {
                    expensiveShops { 
                        id name
                    } 
                }
            }
            """

    static def expectedExpensiveDeferredData = [
            [[id: "exshop-1", name: "ExShop 1"], [id: "exshop-2", name: "ExShop 2"], [id: "exshop-3", name: "ExShop 3"]],
            [[name: "Department 1",products:null, expensiveProducts:null], [name: "Department 2",products:null, expensiveProducts:null], [name: "Department 3",products:null, expensiveProducts:null]],
            [[name: "Department 1", products: [[name: "Product 1"]], expensiveProducts: [[name: "Product 1"]]], [name: "Department 2", products: [[name: "Product 2"]], expensiveProducts: [[name: "Product 2"]]], [name: "Department 3", products: [[name: "Product 3"]], expensiveProducts: [[name: "Product 3"]]]],
            [[name: "Department 4",products:null, expensiveProducts:null], [name: "Department 5",products:null, expensiveProducts:null], [name: "Department 6",products:null, expensiveProducts:null]],
            [[name: "Department 4", products: [[name: "Product 4"]], expensiveProducts: [[name: "Product 4"]]], [name: "Department 5", products: [[name: "Product 5"]], expensiveProducts: [[name: "Product 5"]]], [name: "Department 6", products: [[name: "Product 6"]], expensiveProducts: [[name: "Product 6"]]]],
            [[name: "Department 7",products:null, expensiveProducts:null], [name: "Department 8",products:null, expensiveProducts:null], [name: "Department 9",products:null, expensiveProducts:null]],
            [[name: "Department 7", products: [[name: "Product 7"]], expensiveProducts: [[name: "Product 7"]]], [name: "Department 8", products: [[name: "Product 8"]], expensiveProducts: [[name: "Product 8"]]], [name: "Department 9", products: [[name: "Product 9"]], expensiveProducts: [[name: "Product 9"]]]],
            [[name: "Product 1"]],
            [[name: "Product 1"]],
            [[name: "Product 2"]],
            [[name: "Product 2"]],
            [[name: "Product 3"]],
            [[name: "Product 3"]],
            [[name: "Product 4"]],
            [[name: "Product 4"]],
            [[name: "Product 5"]],
            [[name: "Product 5"]],
            [[name: "Product 6"]],
            [[name: "Product 6"]],
            [[name: "Product 7"]],
            [[name: "Product 7"]],
            [[name: "Product 8"]],
            [[name: "Product 8"]],
            [[name: "Product 9"]],
            [[name: "Product 9"]],
    ]

    static List<Map<String, Object>> getIncrementalResults(IncrementalExecutionResult initialResult) {
        Publisher<DelayedIncrementalPartialResult> deferredResultStream = initialResult.incrementalItemPublisher

        def subscriber = new CapturingSubscriber<DelayedIncrementalPartialResult>()
        deferredResultStream.subscribe(subscriber)
        Awaitility.await().untilTrue(subscriber.isDone())

        return subscriber.getEvents()
                .collect { it.toSpecification() }
    }
}
