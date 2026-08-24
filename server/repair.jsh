String dbPath = System.getenv("DB_PATH");
if (dbPath == null) {
    String userHome = System.getProperty("user.home");
    dbPath = userHome + "\\RestaurantServer\\data\\restaurant.db";
}
String url = "jdbc:sqlite:" + dbPath + "?journal_mode=WAL&foreign_keys=on&busy_timeout=5000";
System.out.println(">> Repairing DB: " + dbPath);
var cfg = org.flywaydb.core.Flyway.configure();
cfg = cfg.dataSource(url, "", "");
cfg = cfg.locations("filesystem:C:\\AppRestaurant\\server\\src\\main\\resources\\db\\migration");
var flyway = cfg.load();
flyway.repair();
System.out.println(">> Repair OK");
flyway.migrate();
System.out.println(">> Migrate OK");
var info = flyway.info();
System.out.println(">> current=" + info.current());
System.out.println(">> pending=" + info.pending());
/exit