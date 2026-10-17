@RunWith(RobolectricTestRunner::class)
// Match the app's compileSdk to avoid Robolectric failing in CI when the requested
// SDK platform is not available.
@Config(sdk = [36])
class ExampleRobolectricTest {
