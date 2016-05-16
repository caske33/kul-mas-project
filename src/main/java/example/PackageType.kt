package example

//TODO variables prices in PackageType of in Variables?
enum class PackageType(val marketPrice: Double) {
    SSD(100.0),
    CAMERA(200.0),
    IPHONE(500.0),
    LAPTOP(1000.0);
}
