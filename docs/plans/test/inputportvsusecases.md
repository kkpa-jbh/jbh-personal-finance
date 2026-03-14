Here is Claude's plan:
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
Fix: Use *UseCase interfaces instead of *InputPort implementations in tests

Violations Found (4 files)

1. FindMonthlyBalanceInputPortTest.java

- private FindMonthlyBalanceInputPort inputPort; → private FindMonthlyBalanceUseCase inputPort;
- Remove import FindMonthlyBalanceInputPort, add import FindMonthlyBalanceUseCase
- Keep instantiation as-is (uses Mockito mocks for services, can't use builder)

2. RegisterMovementExecutionTest.java

- private AddMovementInputPort useCaseInstanceTest; → private AddMovementUseCase useCaseInstanceTest;
- Remove import AddMovementInputPort, add import AddMovementUseCase
- Keep instantiation as-is (uses Mockito mocks, can't use builder)

3. RegisterMovementValidationTest.java

- private AddMovementInputPort registerSimpleMovementInputPort; → private AddMovementUseCase registerSimpleMovementInputPort;
- Remove import AddMovementInputPort, add import AddMovementUseCase
- Keep instantiation as-is (uses Mockito mocks, can't use builder)

4. GetProductMetadataConfigInputPortTest.java

- private GetProductMetadataConfigInputPort inputPort; → private GetProductMetadataConfigUseCase inputPort;
- Remove import GetProductMetadataConfigInputPort, add import GetProductMetadataConfigUseCase
- Add buildGetProductMetadataConfigUseCase() to UseCaseFixtureBuilder and use it in setUp()

Files to Modify

- ...feature/monthlybalance/ports/input/FindMonthlyBalanceInputPortTest.java — change field type + import
- ...feature/movement/usecases/RegisterMovementExecutionTest.java — change field type + import
- ...feature/movement/usecases/RegisterMovementValidationTest.java — change field type + import
- ...feature/product/ports/input/GetProductMetadataConfigInputPortTest.java — change field type + import + use builder
- ...testfixtures/builders/UseCaseFixtureBuilder.java — add buildGetProductMetadataConfigUseCase() method
