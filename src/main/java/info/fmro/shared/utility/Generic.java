package info.fmro.shared.utility;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import info.fmro.shared.enums.ProgramName;
import javafx.scene.control.TreeItem;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.IDN;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

@SuppressWarnings({"WeakerAccess", "UtilityClass", "ClassWithTooManyMethods", "OverlyComplexClass"})
public final class Generic {
    private static final Logger logger = LoggerFactory.getLogger(Generic.class);
    // TLDs last updated 29-06-2019 from http://data.iana.org/TLD/tlds-alpha-by-domain.txt
    @SuppressWarnings("SpellCheckingInspection")
    public static final Set<String> TLDs =
            Set.of("AAA", "AARP", "ABARTH", "ABB", "ABBOTT", "ABBVIE", "ABC", "ABLE", "ABOGADO", "ABUDHABI", "AC", "ACADEMY", "ACCENTURE", "ACCOUNTANT", "ACCOUNTANTS", "ACO", "ACTOR", "AD", "ADAC", "ADS", "ADULT", "AE", "AEG", "AERO", "AETNA", "AF",
                   "AFAMILYCOMPANY", "AFL", "AFRICA", "AG", "AGAKHAN", "AGENCY", "AI", "AIG", "AIGO", "AIRBUS", "AIRFORCE", "AIRTEL", "AKDN", "AL", "ALFAROMEO", "ALIBABA", "ALIPAY", "ALLFINANZ", "ALLSTATE", "ALLY", "ALSACE", "ALSTOM", "AM",
                   "AMERICANEXPRESS", "AMERICANFAMILY", "AMEX", "AMFAM", "AMICA", "AMSTERDAM", "ANALYTICS", "ANDROID", "ANQUAN", "ANZ", "AO", "AOL", "APARTMENTS", "APP", "APPLE", "AQ", "AQUARELLE", "AR", "ARAB", "ARAMCO", "ARCHI", "ARMY", "ARPA",
                   "ART", "ARTE", "AS", "ASDA", "ASIA", "ASSOCIATES", "AT", "ATHLETA", "ATTORNEY", "AU", "AUCTION", "AUDI", "AUDIBLE", "AUDIO", "AUSPOST", "AUTHOR", "AUTO", "AUTOS", "AVIANCA", "AW", "AWS", "AX", "AXA", "AZ", "AZURE", "BA", "BABY",
                   "BAIDU", "BANAMEX", "BANANAREPUBLIC", "BAND", "BANK", "BAR", "BARCELONA", "BARCLAYCARD", "BARCLAYS", "BAREFOOT", "BARGAINS", "BASEBALL", "BASKETBALL", "BAUHAUS", "BAYERN", "BB", "BBC", "BBT", "BBVA", "BCG", "BCN", "BD", "BE",
                   "BEATS", "BEAUTY", "BEER", "BENTLEY", "BERLIN", "BEST", "BESTBUY", "BET", "BF", "BG", "BH", "BHARTI", "BI", "BIBLE", "BID", "BIKE", "BING", "BINGO", "BIO", "BIZ", "BJ", "BLACK", "BLACKFRIDAY", "BLOCKBUSTER", "BLOG", "BLOOMBERG",
                   "BLUE", "BM", "BMS", "BMW", "BN", "BNL", "BNPPARIBAS", "BO", "BOATS", "BOEHRINGER", "BOFA", "BOM", "BOND", "BOO", "BOOK", "BOOKING", "BOSCH", "BOSTIK", "BOSTON", "BOT", "BOUTIQUE", "BOX", "BR", "BRADESCO", "BRIDGESTONE",
                   "BROADWAY", "BROKER", "BROTHER", "BRUSSELS", "BS", "BT", "BUDAPEST", "BUGATTI", "BUILD", "BUILDERS", "BUSINESS", "BUY", "BUZZ", "BV", "BW", "BY", "BZ", "BZH", "CA", "CAB", "CAFE", "CAL", "CALL", "CALVINKLEIN", "CAM", "CAMERA",
                   "CAMP", "CANCERRESEARCH", "CANON", "CAPETOWN", "CAPITAL", "CAPITALONE", "CAR", "CARAVAN", "CARDS", "CARE", "CAREER", "CAREERS", "CARS", "CARTIER", "CASA", "CASE", "CASEIH", "CASH", "CASINO", "CAT", "CATERING", "CATHOLIC", "CBA",
                   "CBN", "CBRE", "CBS", "CC", "CD", "CEB", "CENTER", "CEO", "CERN", "CF", "CFA", "CFD", "CG", "CH", "CHANEL", "CHANNEL", "CHARITY", "CHASE", "CHAT", "CHEAP", "CHINTAI", "CHRISTMAS", "CHROME", "CHRYSLER", "CHURCH", "CI", "CIPRIANI",
                   "CIRCLE", "CISCO", "CITADEL", "CITI", "CITIC", "CITY", "CITYEATS", "CK", "CL", "CLAIMS", "CLEANING", "CLICK", "CLINIC", "CLINIQUE", "CLOTHING", "CLOUD", "CLUB", "CLUBMED", "CM", "CN", "CO", "COACH", "CODES", "COFFEE", "COLLEGE",
                   "COLOGNE", "COM", "COMCAST", "COMMBANK", "COMMUNITY", "COMPANY", "COMPARE", "COMPUTER", "COMSEC", "CONDOS", "CONSTRUCTION", "CONSULTING", "CONTACT", "CONTRACTORS", "COOKING", "COOKINGCHANNEL", "COOL", "COOP", "CORSICA", "COUNTRY",
                   "COUPON", "COUPONS", "COURSES", "CR", "CREDIT", "CREDITCARD", "CREDITUNION", "CRICKET", "CROWN", "CRS", "CRUISE", "CRUISES", "CSC", "CU", "CUISINELLA", "CV", "CW", "CX", "CY", "CYMRU", "CYOU", "CZ", "DABUR", "DAD", "DANCE",
                   "DATA", "DATE", "DATING", "DATSUN", "DAY", "DCLK", "DDS", "DE", "DEAL", "DEALER", "DEALS", "DEGREE", "DELIVERY", "DELL", "DELOITTE", "DELTA", "DEMOCRAT", "DENTAL", "DENTIST", "DESI", "DESIGN", "DEV", "DHL", "DIAMONDS", "DIET",
                   "DIGITAL", "DIRECT", "DIRECTORY", "DISCOUNT", "DISCOVER", "DISH", "DIY", "DJ", "DK", "DM", "DNP", "DO", "DOCS", "DOCTOR", "DODGE", "DOG", "DOMAINS", "DOT", "DOWNLOAD", "DRIVE", "DTV", "DUBAI", "DUCK", "DUNLOP", "DUNS", "DUPONT",
                   "DURBAN", "DVAG", "DVR", "DZ", "EARTH", "EAT", "EC", "ECO", "EDEKA", "EDU", "EDUCATION", "EE", "EG", "EMAIL", "EMERCK", "ENERGY", "ENGINEER", "ENGINEERING", "ENTERPRISES", "EPSON", "EQUIPMENT", "ER", "ERICSSON", "ERNI", "ES",
                   "ESQ", "ESTATE", "ESURANCE", "ET", "ETISALAT", "EU", "EUROVISION", "EUS", "EVENTS", "EVERBANK", "EXCHANGE", "EXPERT", "EXPOSED", "EXPRESS", "EXTRASPACE", "FAGE", "FAIL", "FAIRWINDS", "FAITH", "FAMILY", "FAN", "FANS", "FARM",
                   "FARMERS", "FASHION", "FAST", "FEDEX", "FEEDBACK", "FERRARI", "FERRERO", "FI", "FIAT", "FIDELITY", "FIDO", "FILM", "FINAL", "FINANCE", "FINANCIAL", "FIRE", "FIRESTONE", "FIRMDALE", "FISH", "FISHING", "FIT", "FITNESS", "FJ", "FK",
                   "FLICKR", "FLIGHTS", "FLIR", "FLORIST", "FLOWERS", "FLY", "FM", "FO", "FOO", "FOOD", "FOODNETWORK", "FOOTBALL", "FORD", "FOREX", "FORSALE", "FORUM", "FOUNDATION", "FOX", "FR", "FREE", "FRESENIUS", "FRL", "FROGANS", "FRONTDOOR",
                   "FRONTIER", "FTR", "FUJITSU", "FUJIXEROX", "FUN", "FUND", "FURNITURE", "FUTBOL", "FYI", "GA", "GAL", "GALLERY", "GALLO", "GALLUP", "GAME", "GAMES", "GAP", "GARDEN", "GB", "GBIZ", "GD", "GDN", "GE", "GEA", "GENT", "GENTING",
                   "GEORGE", "GF", "GG", "GGEE", "GH", "GI", "GIFT", "GIFTS", "GIVES", "GIVING", "GL", "GLADE", "GLASS", "GLE", "GLOBAL", "GLOBO", "GM", "GMAIL", "GMBH", "GMO", "GMX", "GN", "GODADDY", "GOLD", "GOLDPOINT", "GOLF", "GOO", "GOODYEAR",
                   "GOOG", "GOOGLE", "GOP", "GOT", "GOV", "GP", "GQ", "GR", "GRAINGER", "GRAPHICS", "GRATIS", "GREEN", "GRIPE", "GROCERY", "GROUP", "GS", "GT", "GU", "GUARDIAN", "GUCCI", "GUGE", "GUIDE", "GUITARS", "GURU", "GW", "GY", "HAIR",
                   "HAMBURG", "HANGOUT", "HAUS", "HBO", "HDFC", "HDFCBANK", "HEALTH", "HEALTHCARE", "HELP", "HELSINKI", "HERE", "HERMES", "HGTV", "HIPHOP", "HISAMITSU", "HITACHI", "HIV", "HK", "HKT", "HM", "HN", "HOCKEY", "HOLDINGS", "HOLIDAY",
                   "HOMEDEPOT", "HOMEGOODS", "HOMES", "HOMESENSE", "HONDA", "HORSE", "HOSPITAL", "HOST", "HOSTING", "HOT", "HOTELES", "HOTELS", "HOTMAIL", "HOUSE", "HOW", "HR", "HSBC", "HT", "HU", "HUGHES", "HYATT", "HYUNDAI", "IBM", "ICBC", "ICE",
                   "ICU", "ID", "IE", "IEEE", "IFM", "IKANO", "IL", "IM", "IMAMAT", "IMDB", "IMMO", "IMMOBILIEN", "IN", "INC", "INDUSTRIES", "INFINITI", "INFO", "ING", "INK", "INSTITUTE", "INSURANCE", "INSURE", "INT", "INTEL", "INTERNATIONAL",
                   "INTUIT", "INVESTMENTS", "IO", "IPIRANGA", "IQ", "IR", "IRISH", "IS", "ISELECT", "ISMAILI", "IST", "ISTANBUL", "IT", "ITAU", "ITV", "IVECO", "JAGUAR", "JAVA", "JCB", "JCP", "JE", "JEEP", "JETZT", "JEWELRY", "JIO", "JLL", "JM",
                   "JMP", "JNJ", "JO", "JOBS", "JOBURG", "JOT", "JOY", "JP", "JPMORGAN", "JPRS", "JUEGOS", "JUNIPER", "KAUFEN", "KDDI", "KE", "KERRYHOTELS", "KERRYLOGISTICS", "KERRYPROPERTIES", "KFH", "KG", "KH", "KI", "KIA", "KIM", "KINDER",
                   "KINDLE", "KITCHEN", "KIWI", "KM", "KN", "KOELN", "KOMATSU", "KOSHER", "KP", "KPMG", "KPN", "KR", "KRD", "KRED", "KUOKGROUP", "KW", "KY", "KYOTO", "KZ", "LA", "LACAIXA", "LADBROKES", "LAMBORGHINI", "LAMER", "LANCASTER", "LANCIA",
                   "LANCOME", "LAND", "LANDROVER", "LANXESS", "LASALLE", "LAT", "LATINO", "LATROBE", "LAW", "LAWYER", "LB", "LC", "LDS", "LEASE", "LECLERC", "LEFRAK", "LEGAL", "LEGO", "LEXUS", "LGBT", "LI", "LIAISON", "LIDL", "LIFE",
                   "LIFEINSURANCE", "LIFESTYLE", "LIGHTING", "LIKE", "LILLY", "LIMITED", "LIMO", "LINCOLN", "LINDE", "LINK", "LIPSY", "LIVE", "LIVING", "LIXIL", "LK", "LLC", "LOAN", "LOANS", "LOCKER", "LOCUS", "LOFT", "LOL", "LONDON", "LOTTE",
                   "LOTTO", "LOVE", "LPL", "LPLFINANCIAL", "LR", "LS", "LT", "LTD", "LTDA", "LU", "LUNDBECK", "LUPIN", "LUXE", "LUXURY", "LV", "LY", "MA", "MACYS", "MADRID", "MAIF", "MAISON", "MAKEUP", "MAN", "MANAGEMENT", "MANGO", "MAP", "MARKET",
                   "MARKETING", "MARKETS", "MARRIOTT", "MARSHALLS", "MASERATI", "MATTEL", "MBA", "MC", "MCKINSEY", "MD", "ME", "MED", "MEDIA", "MEET", "MELBOURNE", "MEME", "MEMORIAL", "MEN", "MENU", "MERCKMSD", "METLIFE", "MG", "MH", "MIAMI",
                   "MICROSOFT", "MIL", "MINI", "MINT", "MIT", "MITSUBISHI", "MK", "ML", "MLB", "MLS", "MM", "MMA", "MN", "MO", "MOBI", "MOBILE", "MOBILY", "MODA", "MOE", "MOI", "MOM", "MONASH", "MONEY", "MONSTER", "MOPAR", "MORMON", "MORTGAGE",
                   "MOSCOW", "MOTO", "MOTORCYCLES", "MOV", "MOVIE", "MOVISTAR", "MP", "MQ", "MR", "MS", "MSD", "MT", "MTN", "MTR", "MU", "MUSEUM", "MUTUAL", "MV", "MW", "MX", "MY", "MZ", "NA", "NAB", "NADEX", "NAGOYA", "NAME", "NATIONWIDE",
                   "NATURA", "NAVY", "NBA", "NC", "NE", "NEC", "NET", "NETBANK", "NETFLIX", "NETWORK", "NEUSTAR", "NEW", "NEWHOLLAND", "NEWS", "NEXT", "NEXTDIRECT", "NEXUS", "NF", "NFL", "NG", "NGO", "NHK", "NI", "NICO", "NIKE", "NIKON", "NINJA",
                   "NISSAN", "NISSAY", "NL", "NO", "NOKIA", "NORTHWESTERNMUTUAL", "NORTON", "NOW", "NOWRUZ", "NOWTV", "NP", "NR", "NRA", "NRW", "NTT", "NU", "NYC", "NZ", "OBI", "OBSERVER", "OFF", "OFFICE", "OKINAWA", "OLAYAN", "OLAYANGROUP",
                   "OLDNAVY", "OLLO", "OM", "OMEGA", "ONE", "ONG", "ONL", "ONLINE", "ONYOURSIDE", "OOO", "OPEN", "ORACLE", "ORANGE", "ORG", "ORGANIC", "ORIGINS", "OSAKA", "OTSUKA", "OTT", "OVH", "PA", "PAGE", "PANASONIC", "PARIS", "PARS",
                   "PARTNERS", "PARTS", "PARTY", "PASSAGENS", "PAY", "PCCW", "PE", "PET", "PF", "PFIZER", "PG", "PH", "PHARMACY", "PHD", "PHILIPS", "PHONE", "PHOTO", "PHOTOGRAPHY", "PHOTOS", "PHYSIO", "PIAGET", "PICS", "PICTET", "PICTURES", "PID",
                   "PIN", "PING", "PINK", "PIONEER", "PIZZA", "PK", "PL", "PLACE", "PLAY", "PLAYSTATION", "PLUMBING", "PLUS", "PM", "PN", "PNC", "POHL", "POKER", "POLITIE", "PORN", "POST", "PR", "PRAMERICA", "PRAXI", "PRESS", "PRIME", "PRO", "PROD",
                   "PRODUCTIONS", "PROF", "PROGRESSIVE", "PROMO", "PROPERTIES", "PROPERTY", "PROTECTION", "PRU", "PRUDENTIAL", "PS", "PT", "PUB", "PW", "PWC", "PY", "QA", "QPON", "QUEBEC", "QUEST", "QVC", "RACING", "RADIO", "RAID", "RE", "READ",
                   "REALESTATE", "REALTOR", "REALTY", "RECIPES", "RED", "REDSTONE", "REDUMBRELLA", "REHAB", "REISE", "REISEN", "REIT", "RELIANCE", "REN", "RENT", "RENTALS", "REPAIR", "REPORT", "REPUBLICAN", "REST", "RESTAURANT", "REVIEW", "REVIEWS",
                   "REXROTH", "RICH", "RICHARDLI", "RICOH", "RIGHTATHOME", "RIL", "RIO", "RIP", "RMIT", "RO", "ROCHER", "ROCKS", "RODEO", "ROGERS", "ROOM", "RS", "RSVP", "RU", "RUGBY", "RUHR", "RUN", "RW", "RWE", "RYUKYU", "SA", "SAARLAND", "SAFE",
                   "SAFETY", "SAKURA", "SALE", "SALON", "SAMSCLUB", "SAMSUNG", "SANDVIK", "SANDVIKCOROMANT", "SANOFI", "SAP", "SARL", "SAS", "SAVE", "SAXO", "SB", "SBI", "SBS", "SC", "SCA", "SCB", "SCHAEFFLER", "SCHMIDT", "SCHOLARSHIPS", "SCHOOL",
                   "SCHULE", "SCHWARZ", "SCIENCE", "SCJOHNSON", "SCOR", "SCOT", "SD", "SE", "SEARCH", "SEAT", "SECURE", "SECURITY", "SEEK", "SELECT", "SENER", "SERVICES", "SES", "SEVEN", "SEW", "SEX", "SEXY", "SFR", "SG", "SH", "SHANGRILA", "SHARP",
                   "SHAW", "SHELL", "SHIA", "SHIKSHA", "SHOES", "SHOP", "SHOPPING", "SHOUJI", "SHOW", "SHOWTIME", "SHRIRAM", "SI", "SILK", "SINA", "SINGLES", "SITE", "SJ", "SK", "SKI", "SKIN", "SKY", "SKYPE", "SL", "SLING", "SM", "SMART", "SMILE",
                   "SN", "SNCF", "SO", "SOCCER", "SOCIAL", "SOFTBANK", "SOFTWARE", "SOHU", "SOLAR", "SOLUTIONS", "SONG", "SONY", "SOY", "SPACE", "SPORT", "SPOT", "SPREADBETTING", "SR", "SRL", "SRT", "SS", "ST", "STADA", "STAPLES", "STAR", "STARHUB",
                   "STATEBANK", "STATEFARM", "STC", "STCGROUP", "STOCKHOLM", "STORAGE", "STORE", "STREAM", "STUDIO", "STUDY", "STYLE", "SU", "SUCKS", "SUPPLIES", "SUPPLY", "SUPPORT", "SURF", "SURGERY", "SUZUKI", "SV", "SWATCH", "SWIFTCOVER",
                   "SWISS", "SX", "SY", "SYDNEY", "SYMANTEC", "SYSTEMS", "SZ", "TAB", "TAIPEI", "TALK", "TAOBAO", "TARGET", "TATAMOTORS", "TATAR", "TATTOO", "TAX", "TAXI", "TC", "TCI", "TD", "TDK", "TEAM", "TECH", "TECHNOLOGY", "TEL", "TELEFONICA",
                   "TEMASEK", "TENNIS", "TEVA", "TF", "TG", "TH", "THD", "THEATER", "THEATRE", "TIAA", "TICKETS", "TIENDA", "TIFFANY", "TIPS", "TIRES", "TIROL", "TJ", "TJMAXX", "TJX", "TK", "TKMAXX", "TL", "TM", "TMALL", "TN", "TO", "TODAY",
                   "TOKYO", "TOOLS", "TOP", "TORAY", "TOSHIBA", "TOTAL", "TOURS", "TOWN", "TOYOTA", "TOYS", "TR", "TRADE", "TRADING", "TRAINING", "TRAVEL", "TRAVELCHANNEL", "TRAVELERS", "TRAVELERSINSURANCE", "TRUST", "TRV", "TT", "TUBE", "TUI",
                   "TUNES", "TUSHU", "TV", "TVS", "TW", "TZ", "UA", "UBANK", "UBS", "UCONNECT", "UG", "UK", "UNICOM", "UNIVERSITY", "UNO", "UOL", "UPS", "US", "UY", "UZ", "VA", "VACATIONS", "VANA", "VANGUARD", "VC", "VE", "VEGAS", "VENTURES",
                   "VERISIGN", "VERSICHERUNG", "VET", "VG", "VI", "VIAJES", "VIDEO", "VIG", "VIKING", "VILLAS", "VIN", "VIP", "VIRGIN", "VISA", "VISION", "VISTAPRINT", "VIVA", "VIVO", "VLAANDEREN", "VN", "VODKA", "VOLKSWAGEN", "VOLVO", "VOTE",
                   "VOTING", "VOTO", "VOYAGE", "VU", "VUELOS", "WALES", "WALMART", "WALTER", "WANG", "WANGGOU", "WARMAN", "WATCH", "WATCHES", "WEATHER", "WEATHERCHANNEL", "WEBCAM", "WEBER", "WEBSITE", "WED", "WEDDING", "WEIBO", "WEIR", "WF",
                   "WHOSWHO", "WIEN", "WIKI", "WILLIAMHILL", "WIN", "WINDOWS", "WINE", "WINNERS", "WME", "WOLTERSKLUWER", "WOODSIDE", "WORK", "WORKS", "WORLD", "WOW", "WS", "WTC", "WTF", "XBOX", "XEROX", "XFINITY", "XIHUAN", "XIN", "XN--11B4C3D",
                   "XN--1CK2E1B", "XN--1QQW23A", "XN--2SCRJ9C", "XN--30RR7Y", "XN--3BST00M", "XN--3DS443G", "XN--3E0B707E", "XN--3HCRJ9C", "XN--3OQ18VL8PN36A", "XN--3PXU8K", "XN--42C2D9A", "XN--45BR5CYL", "XN--45BRJ9C", "XN--45Q11C", "XN--4GBRIM",
                   "XN--54B7FTA0CC", "XN--55QW42G", "XN--55QX5D", "XN--5SU34J936BGSG", "XN--5TZM5G", "XN--6FRZ82G", "XN--6QQ986B3XL", "XN--80ADXHKS", "XN--80AO21A", "XN--80AQECDR1A", "XN--80ASEHDB", "XN--80ASWG", "XN--8Y0A063A", "XN--90A3AC",
                   "XN--90AE", "XN--90AIS", "XN--9DBQ2A", "XN--9ET52U", "XN--9KRT00A", "XN--B4W605FERD", "XN--BCK1B9A5DRE4C", "XN--C1AVG", "XN--C2BR7G", "XN--CCK2B3B", "XN--CG4BKI", "XN--CLCHC0EA0B2G2A9GCD", "XN--CZR694B", "XN--CZRS0T",
                   "XN--CZRU2D", "XN--D1ACJ3B", "XN--D1ALF", "XN--E1A4C", "XN--ECKVDTC9D", "XN--EFVY88H", "XN--ESTV75G", "XN--FCT429K", "XN--FHBEI", "XN--FIQ228C5HS", "XN--FIQ64B", "XN--FIQS8S", "XN--FIQZ9S", "XN--FJQ720A", "XN--FLW351E",
                   "XN--FPCRJ9C3D", "XN--FZC2C9E2C", "XN--FZYS8D69UVGM", "XN--G2XX48C", "XN--GCKR3F0F", "XN--GECRJ9C", "XN--GK3AT1E", "XN--H2BREG3EVE", "XN--H2BRJ9C", "XN--H2BRJ9C8C", "XN--HXT814E", "XN--I1B6B1A6A2E", "XN--IMR513N", "XN--IO0A7I",
                   "XN--J1AEF", "XN--J1AMH", "XN--J6W193G", "XN--JLQ61U9W7B", "XN--JVR189M", "XN--KCRX77D1X4A", "XN--KPRW13D", "XN--KPRY57D", "XN--KPU716F", "XN--KPUT3I", "XN--L1ACC", "XN--LGBBAT1AD8J", "XN--MGB9AWBF", "XN--MGBA3A3EJT",
                   "XN--MGBA3A4F16A", "XN--MGBA7C0BBN0A", "XN--MGBAAKC7DVF", "XN--MGBAAM7A8H", "XN--MGBAB2BD", "XN--MGBAH1A3HJKRD", "XN--MGBAI9AZGQP6J", "XN--MGBAYH7GPA", "XN--MGBB9FBPOB", "XN--MGBBH1A", "XN--MGBBH1A71E", "XN--MGBC0A9AZCG",
                   "XN--MGBCA7DZDO", "XN--MGBERP4A5D4AR", "XN--MGBGU82A", "XN--MGBI4ECEXP", "XN--MGBPL2FH", "XN--MGBT3DHD", "XN--MGBTX2B", "XN--MGBX4CD0AB", "XN--MIX891F", "XN--MK1BU44C", "XN--MXTQ1M", "XN--NGBC5AZD", "XN--NGBE9E0A", "XN--NGBRX",
                   "XN--NODE", "XN--NQV7F", "XN--NQV7FS00EMA", "XN--NYQY26A", "XN--O3CW4H", "XN--OGBPF8FL", "XN--OTU796D", "XN--P1ACF", "XN--P1AI", "XN--PBT977C", "XN--PGBS0DH", "XN--PSSY2U", "XN--Q9JYB4C", "XN--QCKA1PMC", "XN--QXAM", "XN--RHQV96G",
                   "XN--ROVU88B", "XN--RVC1E0AM3E", "XN--S9BRJ9C", "XN--SES554G", "XN--T60B56A", "XN--TCKWE", "XN--TIQ49XQYJ", "XN--UNUP4Y", "XN--VERMGENSBERATER-CTB", "XN--VERMGENSBERATUNG-PWB", "XN--VHQUV", "XN--VUQ861B", "XN--W4R85EL8FHU5DNRA",
                   "XN--W4RS40L", "XN--WGBH1C", "XN--WGBL6A", "XN--XHQ521B", "XN--XKC2AL3HYE2A", "XN--XKC2DL3A5EE0H", "XN--Y9A3AQ", "XN--YFRO4I67O", "XN--YGBI2AMMX", "XN--ZFR164B", "XXX", "XYZ", "YACHTS", "YAHOO", "YAMAXUN", "YANDEX", "YE",
                   "YODOBASHI", "YOGA", "YOKOHAMA", "YOU", "YOUTUBE", "YT", "YUN", "ZA", "ZAPPOS", "ZARA", "ZERO", "ZIP", "ZM", "ZONE", "ZUERICH", "ZW");
    public static final AtomicReference<ProgramName> programName = new AtomicReference<>();
    public static final Pattern SPACE_PATTERN_COMPILE = Pattern.compile("\\s+");
    public static final String[] EMPTY_STRING_ARRAY = {};
    public static final String US_ASCII_CHARSET = "US-ASCII", UTF8_CHARSET = "UTF-8", UTF16_CHARSET = "UTF-16";
    public static final long DAY_LENGTH_MILLISECONDS = 24L * 60L * 60L * 1_000L, HOUR_LENGTH_MILLISECONDS = 60L * 60L * 1_000L, MINUTE_LENGTH_MILLISECONDS = 60L * 1_000L, MEGABYTE = 1_024L << 10; // 1_024L * 1_024L
    public static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("UTC"), BUCHAREST_TIMEZONE = TimeZone.getTimeZone("Europe/Bucharest");
    public static final AlreadyPrintedMap alreadyPrintedMap = new AlreadyPrintedMap();
    public static final char[] ZERO_LENGTH_CHARS_ARRAY = new char[0];

    private Generic() {
    }

    public static int getClosestNumber(final int mainValue, final int... closeNumbers) {
        int chosenNumber = mainValue;
        if (closeNumbers == null) {
            logger.error("null closeNumbers in getClosestNumber for: {}", mainValue);
        } else {
            int minDifference = Integer.MAX_VALUE;
            for (final int number : closeNumbers) {
                final int difference = Math.abs(number - mainValue);
                if (difference < minDifference) {
                    minDifference = difference;
                    chosenNumber = number;
                } else { // number not close enough, nothing to be done
                }
            }
        }
        return chosenNumber;
    }

    public static double keepDoubleWithinRange(final double value) {
        final double min = -1d;
        final double max = 9_999_999d;
        return keepDoubleWithinRange(value, min, max);
    }

    public static double keepDoubleWithinRange(final double value, final double min, final double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean objectInstanceOf(final Class<?> clazz, final Object object) {
        final boolean isClassOrSubclass;

        if (clazz == null || object == null) {
            logger.error("null parameter in objectInstanceOf: {} {}", clazz, object);
            isClassOrSubclass = false;
        } else {
//            isClassOrSubclass = clazz.isAssignableFrom(object.getClass());
            isClassOrSubclass = clazz.isInstance(object);
        }
        return isClassOrSubclass;
    }

    public static <T> void updateObject(final T mainObject, final T updateSource) {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE); // remove visibility of everything, including getters/setters
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY); // add full visibility for fields
        final ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        final byte[] byteArray;
        try {
            byteArray = ow.writeValueAsBytes(updateSource);
            final ObjectReader reader = mapper.readerForUpdating(mainObject);
            reader.readValue(byteArray);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException e) {
            //noinspection NestedConditionalExpression
            final Class<?> clazz = mainObject == null ? updateSource == null ? null : updateSource.getClass() : mainObject.getClass();
            logger.error("IOException in updateObject for: {} {} {}", clazz, objectToString(mainObject), objectToString(updateSource), e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    public static KeyManager[] getKeyManagers(final String keyStoreFileName, final String keyStorePassword, final String keyStoreType) {
        if (keyStoreFileName == null || keyStorePassword == null || keyStoreType == null) { // the method will continue anyway
            logger.error("null argument in getKeyManagers for: {} {} {}", keyStoreFileName, keyStorePassword, keyStoreType);
        } else { // nothing to be done on this branch, this is a simple null check
        }

        FileInputStream keyStoreFileInputStream = null;
        KeyManagerFactory keyManagerFactory = null;
        try {
            final File keyFile = new File(keyStoreFileName);
            keyStoreFileInputStream = new FileInputStream(keyFile);

            final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(keyStoreFileInputStream, keyStorePassword == null ? ZERO_LENGTH_CHARS_ARRAY : keyStorePassword.toCharArray());
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword == null ? ZERO_LENGTH_CHARS_ARRAY : keyStorePassword.toCharArray());
        } catch (@SuppressWarnings({"OverlyBroadCatchBlock", "ProhibitedExceptionCaught"}) KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | NullPointerException exception) {
            logger.error("STRANGE ERROR inside getKeyManagers", exception);
        } finally {
            closeObject(keyStoreFileInputStream);
        }
        return keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers();
    }

    public static double parseDouble(final String initialString) {
        double result = Double.NaN;
        if (initialString == null) { // value remains NaN, nothing to do
        } else {
            try {
                result = Double.parseDouble(initialString);
            } catch (NumberFormatException e) {
                logger.error("NumberFormatException in parseDouble for: {}", initialString, e);
            }
        }
        return result;
    }

    public static String[] splitStringAroundSpaces(@NotNull final String initialString) { // trims and splits a string around the spaces; spaces themselves will be removed and I shouldn't get any empty strings
        final String[] result = SPACE_PATTERN_COMPILE.split(initialString.trim());
        return Arrays.equals(new String[]{""}, result) ? EMPTY_STRING_ARRAY : result;
    }

    @Contract(pure = true)
    public static int booleanToInt(final boolean boo) {
        return boo ? 1 : 0;
    }

    public static double roundDouble(final double value, final int places, final RoundingMode roundingMode) {
        if (places < 0) {
            logger.error("negative places in roundDouble: {} {} {}", value, places, roundingMode);
        } else { // no error, the method will continue, nothing to be done on branch
        }

        final BigDecimal bd = new BigDecimal(value);
        final BigDecimal result = bd.setScale(Math.max(places, 0), roundingMode);
        return result.doubleValue();
    }

    public static double roundDouble(final double value, final int places) {
        return roundDouble(value, places, RoundingMode.HALF_DOWN);
    }

    public static double roundDoubleAmount(final double value) {
        if (value < 0d) { // values should always be positive, as these are amount to be placed on bets, but the method works fine with negative
            logger.error("negative value in roundDoubleAmount: {}", value);
        } else { // no error, nothing to be done on branch
        }

        return roundDouble(value, 2, RoundingMode.HALF_DOWN);
    }

    public static int getMiddleIndex(final CharSequence mainString, final CharSequence subString) {
        final int result;
        if (mainString == null || subString == null) {
            logger.error("null mainString in getMiddleIndex for: {} {}", mainString, subString);
            result = -1;
        } else {
            final int nMatches = StringUtils.countMatches(mainString, subString);
            final int middle = (nMatches + 1) / 2;
            result = StringUtils.ordinalIndexOf(mainString, subString, middle);
        }

        return result;
    }

    public static <T> T createAndFill(final Class<T> clazz)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        return createAndFill(clazz, 0);
    }

    public static <T> T createAndFill(@NotNull final Class<T> clazz, final int recursionCounter)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // fills a class with random value for fields, using reflection; in case of no arguments in the constructor; used in tests
        // final or static fields are not touched; also this might not work with every class, depending on field types; and it only works with classes that can be instantiated by this method

        @Nullable T instance;
        try {
            instance = clazz.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            instance = null;
            logger.warn("class {} doesn't have constructor without arguments in Generic.createAndFill", clazz.getSimpleName());
        }
        if (instance != null) {
            for (final Field field : clazz.getDeclaredFields()) {
                final int fieldModifiers = field.getModifiers();
                if (Modifier.isFinal(fieldModifiers) || Modifier.isStatic(fieldModifiers)) {
                    // I won't touch final or static fields; nothing to be done
                } else {
                    final Object value = getRandomValueForField(field, recursionCounter); // recursionCounter gets increased in getRandomValueForField
                    field.set(instance, value);
                }
            }
        } else { // instance == null, error message was already printed, nothing to be done
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    public static <T> T createAndFillWithPrimitiveCheck(@NotNull final Class<T> clazz, final int recursionCounter)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        @Nullable T instance;
        final SecureRandom random = new SecureRandom();

        if (clazz.isEnum()) {
            final Object[] enumValues = clazz.getEnumConstants();
            instance = (T) enumValues[random.nextInt(enumValues.length)];
        } else if (clazz.equals(Boolean.TYPE) || clazz.equals(Boolean.class)) {
            instance = (T) Boolean.valueOf(random.nextBoolean());
        } else if (clazz.equals(Double.TYPE) || clazz.equals(Double.class)) {
            instance = (T) Double.valueOf(random.nextDouble());
        } else if (clazz.equals(Float.TYPE) || clazz.equals(Float.class)) {
            instance = (T) Float.valueOf(random.nextFloat());
        } else if (clazz.equals(Integer.TYPE) || clazz.equals(Integer.class)) {
            instance = (T) Integer.valueOf(random.nextInt());
        } else if (clazz.equals(Long.TYPE) || clazz.equals(Long.class)) {
            instance = (T) Long.valueOf(random.nextLong());
        } else if (clazz.equals(String.class)) {
            instance = (T) UUID.randomUUID().toString();
        } else if (clazz.equals(BigInteger.class)) {
            instance = (T) BigInteger.valueOf(random.nextLong());
        } else if (clazz.equals(Date.class)) {
            // Get an Epoch value roughly between 1940 and 2040
            // -946771200000L = January 1, 1940
            // Add up to approx 100 years to it (using modulus on the next long)
            instance = (T) new Date(-946_771_200_000L + (Math.abs(random.nextLong()) % (100L * 365L * 24L * 60L * 60L * 1_000L)));
        } else {
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (NoSuchMethodException e) {
                instance = null;
                logger.warn("class {} doesn't have constructor without arguments in Generic.createAndFillWithPrimitiveCheck", clazz.getSimpleName());
            }
            if (instance != null) {
                for (final Field field : clazz.getDeclaredFields()) {
                    final int fieldModifiers = field.getModifiers();
                    if (Modifier.isFinal(fieldModifiers) || Modifier.isStatic(fieldModifiers)) {
                        // I won't touch final or static fields; nothing to be done
                    } else {
                        final Object value = getRandomValueForField(field, recursionCounter); // recursionCounter gets increased in getRandomValueForField
                        field.set(instance, value);
                    }
                }
            } else { // instance == null, error message was already printed, nothing to be done
            }
        }

        return instance;
    }

    public static void fillRandom(final Object instance)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        fillRandom(instance, 0);
    }

    public static void fillRandom(@NotNull final Object instance, final int recursionCounter)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // fills a class object with random value for fields, using reflection; used in tests
        // final or static fields are not touched; also this might not work with every class, depending on field types
        for (final Field field : instance.getClass().getDeclaredFields()) {
            final int fieldModifiers = field.getModifiers();
            if (Modifier.isFinal(fieldModifiers) || Modifier.isStatic(fieldModifiers)) {
                // I won't touch final or static fields; nothing to be done
            } else {
                final Object value = getRandomValueForField(field, recursionCounter); // recursionCounter gets increased in getRandomValueForField
                field.set(instance, value);
            }
        }
    }

    @SuppressWarnings("NestedConditionalExpression")
    public static Object getRandomValueForField(@NotNull final Field field, final int recursionCounter)
            throws IllegalAccessException, InvocationTargetException, InstantiationException { // used in tests; for field null will throw null exception, as null is not acceptable
        @Nullable final Object result;
        field.setAccessible(true);
        final Class<?> type = field.getType();

        if (recursionCounter < 0 || recursionCounter > 10) {
            logger.error("bogus recursionCounter in getRandomValueForField: {} {}", recursionCounter, type);
            result = type.isPrimitive() ? type.equals(Boolean.TYPE) ? false : 0 : null;
        } else if (recursionCounter == 10) {
            logger.warn("recursionCounter maximum reached in getRandomValueForField: {} {}", recursionCounter, type);
            result = type.isPrimitive() ? type.equals(Boolean.TYPE) ? false : 0 : null;
        } else {
            final SecureRandom random = new SecureRandom();

            if (type.isEnum()) {
                final Object[] enumValues = type.getEnumConstants();
                result = enumValues[random.nextInt(enumValues.length)];
            } else if (type.equals(Boolean.TYPE) || type.equals(Boolean.class)) {
                result = random.nextBoolean();
            } else if (type.equals(Double.TYPE) || type.equals(Double.class)) {
                result = random.nextDouble();
            } else if (type.equals(Float.TYPE) || type.equals(Float.class)) {
                result = random.nextFloat();
            } else if (type.equals(Integer.TYPE) || type.equals(Integer.class)) {
                result = random.nextInt();
            } else if (type.equals(Long.TYPE) || type.equals(Long.class)) {
                result = random.nextLong();
            } else if (type.equals(String.class)) {
                result = UUID.randomUUID().toString();
            } else if (type.equals(BigInteger.class)) {
                result = BigInteger.valueOf(random.nextLong());
            } else if (type.equals(Date.class)) {
                // Get an Epoch value roughly between 1940 and 2040
                // -946771200000L = January 1, 1940
                // Add up to approx 100 years to it (using modulus on the next long)
                result = new Date(-946_771_200_000L + (Math.abs(random.nextLong()) % (100L * 365L * 24L * 60L * 60L * 1_000L)));
            } else if (type.equals(ArrayList.class)) {
                final ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                final Class<?> parameterClass = convertTypeToClass(parameterizedType.getActualTypeArguments()[0]);
                final int randomListSize = random.nextInt(16);
                final List<Object> arrayList = new ArrayList<>(randomListSize);
                for (int i = 0; i < randomListSize; i++) {
                    arrayList.add(i, createAndFillWithPrimitiveCheck(parameterClass, recursionCounter + 1));
                }
                result = arrayList;
            } else if (type.equals(LinkedHashMap.class)) {
                final ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                final Type[] parameterTypes = parameterizedType.getActualTypeArguments();
                final Class<?> parameterClass0 = convertTypeToClass(parameterTypes[0]);
                final Class<?> parameterClass1 = convertTypeToClass(parameterTypes[1]);
                final int randomListSize = random.nextInt(16);
                final Map<Object, Object> map = new LinkedHashMap<>(randomListSize);
                for (int i = 0; i < randomListSize; i++) {
                    map.put(createAndFillWithPrimitiveCheck(parameterClass0, recursionCounter + 1), createAndFillWithPrimitiveCheck(parameterClass1, recursionCounter + 1));
                }
                result = map;
            } else {
                result = createAndFill(type, recursionCounter + 1);
            }
        }

        return result;
    }

    @Contract(pure = true)
    public static Class<?> convertTypeToClass(final Type type) {
        Class<?> result;
        try {
            result = (Class<?>) type;
        } catch (ClassCastException e) { // normal, sometimes type is not a class, but a generic
            result = Object.class;
        }
        return result;
    }

    public static String createStringFromCodes(final int... codes) {
        @Nullable final String returnString;

        if (codes == null) {
            returnString = null;
        } else {
            final int codesSize = codes.length;
            final StringBuilder stringBuilder = new StringBuilder(codesSize);
            for (final int code : codes) {
                stringBuilder.appendCodePoint(code);
            }

            returnString = stringBuilder.toString();
        }

        return returnString;
    }

    @Contract(pure = true)
    public static <T> T getEqualElementFromSet(final Iterable<? extends T> set, final T searchedElement) {
        T returnElement = null;

        if (set == null || searchedElement == null) { // will just return null
        } else {
            for (final T iteratorElement : set) {
                if (searchedElement.equals(iteratorElement)) {
                    returnElement = iteratorElement;
                    break; // break from for
                }
            }
        }
        return returnElement;
    }

    @Contract("null -> null")
    public static String getStringCodePointValues(final String initialString) {
        @Nullable final String result;
        if (initialString == null) {
            result = null;
        } else {
            final int initialStringLength = initialString.length();
            final StringBuilder returnStringBuilder = new StringBuilder(6 * initialStringLength);

            int index = 0, whileCounter = 0;
            while (index < initialStringLength && whileCounter < initialStringLength) {
                final int codePoint = initialString.codePointAt(index);
                index += Character.charCount(codePoint);
                returnStringBuilder.append(codePoint);
                if (index < initialStringLength) {
                    returnStringBuilder.append(" ");
                }
                whileCounter++;
            }
            if (index < initialStringLength) {
                logger.error("bad loop exit in getStringCodePointValues for: {} {} {} {}", initialString, initialStringLength, index, whileCounter);
            }

            result = returnStringBuilder.toString();
        }
        return result;
    }

    //    @SuppressWarnings("AssignmentToForLoopParameter")
//    public static String escapeString(String initialString) {
//        final int initialStringLength = initialString.length();
//        final StringBuilder returnStringBuilder = new StringBuilder(6 * initialStringLength);
//
//        for (int i = 0; i < initialStringLength; i++) {
//            final int codePoint = Character.codePointAt(initialString, i);
//            final int charCount = Character.charCount(codePoint);
//            if (charCount > 1) {
//                i += charCount - 1; // charCount should be 2
//                if (i >= initialStringLength) {
//                    logger.error("truncated unexpectedly in Generic.escapeString for: {}", initialString);
////                    throw new IllegalArgumentException("truncated unexpectedly");
//                }
//            }
//
//            if (codePoint < 128) {
//                returnStringBuilder.appendCodePoint(codePoint);
//            } else {
//                returnStringBuilder.append(String.format("\\u%x", codePoint));
//            }
//        } // end for
//
//        return returnStringBuilder.toString();
//    }
    public static String properTimeStamp() {
        return properTimeStamp(System.currentTimeMillis());
    }

    public static String properTimeStamp(final long millis) {
        String returnValue = new Timestamp(millis).toString();
        final int length = returnValue.length();
        if (length < 21 || length > 23) {
            logger.error("wrong timeStamp length {} in properTimeStamp: {}", length, returnValue);
        } else if (length == 21) {
            returnValue += "00";
        } else if (length == 22) {
            returnValue += "0";
        } else { // length 23, nothing to be done
        }

        return returnValue;
    }

    public static void changeDefaultCharset(final String newCharset) {
        System.setProperty("file.encoding", newCharset);
        try {
            final Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (NoSuchFieldException noSuchFieldException) {
            logger.error("STRANGE noSuchFieldException in changeDefaultCharset for: {}", newCharset, noSuchFieldException);
        } catch (IllegalAccessException illegalAccessException) {
            logger.error("STRANGE illegalAccessException in changeDefaultCharset for: {}", newCharset, illegalAccessException);
        }
    }

    public static ArrayList<? extends OutputStream> replaceStandardStreams(final String outFileName, final String errFileName, final String logsFolderName) {
        return replaceStandardStreams(outFileName, errFileName, logsFolderName, true); // closed by default, left open for JUnit tests
    }

    @SuppressWarnings("ImplicitDefaultCharsetUsage")
    public static ArrayList<? extends OutputStream> replaceStandardStreams(final String outFileName, final String errFileName, final String logsFolderName, final boolean closeExistingStreams) {
        @Nullable ArrayList<OutputStream> list;
        FileOutputStream outFileOutputStream = null, errFileOutputStream = null;
        PrintStream outPrintStream = null, errPrintStream = null;
        try {
            backupFiles(logsFolderName, true, outFileName, errFileName);
//            new File(logsFolderName).mkdirs();
//
//            File previousFile = new File(outFileName);
//            if (previousFile.exists() && previousFile.length() > 0) { // moving existing out log file to the logs folder
//                previousFile.renameTo(new File(logsFolderName + "/" + Generic.tempFileName("outFile") + ".txt"));
//            }
//            previousFile = new File(errFileName);
//            if (previousFile.exists() && previousFile.length() > 0) { // moving existing err log file to the logs folder
//                previousFile.renameTo(new File(logsFolderName + "/" + Generic.tempFileName("errFile") + ".txt"));
//            }

            list = new ArrayList<>(4);

            outFileOutputStream = new FileOutputStream(outFileName);
            list.add(outFileOutputStream);
            outPrintStream = new PrintStream(outFileOutputStream);
            list.add(outPrintStream);
            errFileOutputStream = new FileOutputStream(errFileName);
            list.add(errFileOutputStream);
            errPrintStream = new PrintStream(errFileOutputStream);
            list.add(errPrintStream);

            @SuppressWarnings("UseOfSystemOutOrSystemErr") final PrintStream existingSystemOut = System.out;
            @SuppressWarnings("UseOfSystemOutOrSystemErr") final PrintStream existingSystemErr = System.err;
//            if (closeExistingStreams) {
//                Generic.closeStandardStreams();
//            } else { // error message printed at the end of method, nothing to be done here
//            }

            System.setOut(outPrintStream);
            System.setErr(errPrintStream);
            if (closeExistingStreams) {
                closeObjects(System.in, existingSystemOut, existingSystemErr);
            } else { // error message printed at the end of method, nothing to be done here
            }
        } catch (FileNotFoundException fileNotFoundException) {
            list = null;
            logger.error("fileNotFoundException in replaceStandardStreams: {} {}", outFileName, errFileName, fileNotFoundException);
            //noinspection ConstantConditions
            closeObjects(outFileOutputStream, errFileOutputStream, outPrintStream, errPrintStream);
        }
        logger.info("have replaced standard streams: out={} err={} logsFolder={}", outFileName, errFileName, logsFolderName);
        if (closeExistingStreams) { // normal behaviour, nothing to be done
        } else {
            logger.error("not closing standardStreams in replaceStandardStreams, this is only OK during testing");
        }

        return list;
    }

    public static void backupFiles(@NotNull final String backupFolderName, @NotNull final String... fileNames) {
        backupFiles(backupFolderName, false, fileNames);
    }

    public static void backupFiles(@NotNull final String backupFolderName, final boolean removeOriginal, @NotNull final String... fileNames) {
        //noinspection ResultOfMethodCallIgnored
        new File(backupFolderName).mkdirs();
        for (final String fileName : fileNames) {
            final File previousFile = new File(fileName);
            if (previousFile.exists() && previousFile.length() > 0L) { // moving existing out log file to the logs folder
                final String fileNameWithoutFolder = previousFile.getName();
                final String backupFileName = tempFileName(backupFolderName + File.separator + fileNameWithoutFolder);
                final File backupFile = new File(backupFileName);
                if (removeOriginal) {
                    //noinspection ResultOfMethodCallIgnored
                    previousFile.renameTo(backupFile);
                } else {
                    try {
                        Files.copy(previousFile.toPath(), backupFile.toPath());
                    } catch (IOException e) {
                        logger.error("IOException in backup file for: {} {} {} {} {}", backupFolderName, removeOriginal, fileName, fileNameWithoutFolder, backupFileName, e);
                    }
                }
            } else { // file does not exist, or is empty; it's sometimes normal
            }
        } // end for
    }

    public static <T> Set<Class<? extends T>> getSubclasses(final String prefix, final Class<T> myInterface) {
        final Reflections reflections = new Reflections(prefix);
        return reflections.getSubTypesOf(myInterface);
    }

    public static <T> void collectionKeepMultiples(final Collection<T> collection, final int minNMultiple) {
        if (minNMultiple <= 1) {
            logger.error("minNMultiple {} should be at least 2 in order to have effect in collectionKeepMultiples", minNMultiple);
        } else {
            final Iterable<T> set = new HashSet<>(collection);
            for (int i = 0; i < minNMultiple - 1; i++) {
                for (final T element : set) {
                    collection.remove(element);
                }
            }
        }
    }

    @Contract(pure = true)
    public static boolean isPowerOfTwo(final long x) { // alternative: ((value & -value) == value)
        return x != 0L && ((x & (x - 1L)) == 0L);
    }

    public static void stringBuilderReplace(@NotNull final StringBuilder stringBuilder, final String initialString) {
        stringBuilder.replace(0, stringBuilder.length(), initialString);
    }

    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    public static double middleValue(final double a, final double b, final double c) {
        return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
    }

    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    public static float middleValue(final float a, final float b, final float c) {
        return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
    }

    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    public static int middleValue(final int a, final int b, final int c) {
        return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
    }

    @SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters")
    public static long middleValue(final long a, final long b, final long c) {
        return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
    }

    public static double truncateDouble(final double doubleValue, final int decimals) {
        final double power10double = StrictMath.pow(10d, decimals);
        @SuppressWarnings("NumericCastThatLosesPrecision") final long power10long = (long) power10double;
        return Math.floor(doubleValue * power10double) / power10long;
    }

    public static String quotedReplaceAll(final String initialString, final String pattern, final String replacement) {
        @Nullable final String result;
        if (initialString != null) {
            final String quotedPattern = Pattern.quote(pattern);
            result = initialString.contains(pattern) ? initialString.replaceAll(quotedPattern, replacement) : initialString;
        } else {
            result = null;
        }

        return result;
    }

    public static double stringMatchChance(final String stringFirst, final String stringSecond) {
        return stringMatchChance(stringFirst, stringSecond, true, true);
    }

    public static double stringMatchChance(final String stringFirst, final String stringSecond, final boolean ignoreCase) {
        return stringMatchChance(stringFirst, stringSecond, ignoreCase, true);
    }

    @SuppressWarnings({"BooleanParameter", "OverlyComplexMethod", "OverlyLongMethod", "OverlyNestedMethod"})
    public static double stringMatchChance(final String stringFirst, final String stringSecond, final boolean ignoreCase, final boolean trimStrings) {
        final double result;

        if (stringFirst == null || stringSecond == null) {
            result = 0d;
        } else {
            String localFirst = stringFirst, localSecond = stringSecond;
            if (ignoreCase) {
                localFirst = localFirst.toLowerCase(Locale.ENGLISH);
                localSecond = localSecond.toLowerCase(Locale.ENGLISH);
            }
            if (trimStrings) {
                localFirst = localFirst.trim();
                localSecond = localSecond.trim();
            }

            if (stringFirst.isEmpty() || stringSecond.isEmpty()) {
                result = 0d;
            } else {
                final int numberOfIterations = 3, matchedChars[] = new int[numberOfIterations], sequencedChars[] = new int[numberOfIterations], firstLength = localFirst.length(), secondLength = localSecond.length();
                final String[] loopLocalFirst = new String[numberOfIterations];
                final String[] loopLocalSecond = new String[numberOfIterations];
                loopLocalFirst[0] = localFirst;
                loopLocalSecond[0] = localSecond;
                loopLocalFirst[1] = backwardString(localFirst);
                loopLocalSecond[1] = backwardString(localSecond);
                loopLocalFirst[2] = backwardWordsString(localFirst);
                loopLocalSecond[2] = localSecond; // for cases of "word1 word2" compare with "word2 word1"

                for (int i = 0; i < numberOfIterations; i++) {
                    int counterFirst = 0, counterSecond = 0;
                    boolean charSequence = false;

                    do {
                        if (loopLocalFirst[i].charAt(counterFirst) == loopLocalSecond[i].charAt(counterSecond)) {
                            if (charSequence) {
                                sequencedChars[i]++;
                            }
                            matchedChars[i]++;
                            counterFirst++;
                            counterSecond++;
                            charSequence = true; // sequence starts, first char in regular sequence is not counted
                        } else {
                            charSequence = false;

                            // search char from first into second
                            int indexFirst = -1, indexSecond = -1, increaseSecond = 0, increaseFirst = 0;

                            for (int increaseCounter = 0; counterFirst + increaseCounter < firstLength; increaseCounter++) {
                                final int tempIndex = loopLocalSecond[i].indexOf(loopLocalFirst[i].charAt(counterFirst + increaseFirst), counterSecond);
                                if (tempIndex >= 0 && (indexSecond < 0 || tempIndex + increaseCounter < indexSecond + increaseFirst)) {
                                    indexSecond = tempIndex;
                                    increaseFirst = increaseCounter;
                                }
                            } // end for
                            for (int increaseCounter = 0; counterSecond + increaseCounter < secondLength; increaseCounter++) {
                                final int tempIndex = loopLocalFirst[i].indexOf(loopLocalSecond[i].charAt(counterSecond + increaseSecond), counterFirst);
                                if (tempIndex >= 0 && (indexFirst < 0 || tempIndex + increaseCounter < indexFirst + increaseSecond)) {
                                    indexFirst = tempIndex;
                                    increaseSecond = increaseCounter;
                                }
                            } // end for

//                            int indexSecond = loopLocalSecond[i].indexOf(loopLocalFirst[i].charAt(counterFirst), counterSecond);
//                            int indexFirst = loopLocalFirst[i].indexOf(loopLocalSecond[i].charAt(counterSecond), counterFirst);
                            if (indexFirst < 0 && indexSecond < 0) { // both chars don't exist in the other string
                                counterFirst++;
                                counterSecond++;
                            } else if (indexFirst < 0) {
                                counterSecond = indexSecond;
                                counterFirst += increaseFirst;
                            } else if (indexSecond < 0) {
                                counterFirst = indexFirst;
                                counterSecond += increaseSecond;
                            } else { // see which index is skipping more chars
                                final int skipFirst = indexFirst - counterFirst + increaseSecond;
                                final int skipSecond = indexSecond - counterSecond + increaseFirst;

                                if (skipFirst < skipSecond) {
                                    counterFirst = indexFirst;
                                    counterSecond += increaseSecond;
                                } else if (skipFirst > skipSecond) {
                                    counterSecond = indexSecond;
                                    counterFirst += increaseFirst;
                                } else { // they're equal
                                    final int remainFirst = Math.min(firstLength - indexFirst, secondLength - counterSecond - increaseSecond);
                                    final int remainSecond = Math.min(secondLength - indexSecond, firstLength - counterFirst - increaseFirst);

                                    if (remainFirst < remainSecond) {
                                        counterSecond = indexSecond;
                                        counterFirst += increaseFirst;
                                    } else if (remainFirst > remainSecond) {
                                        counterFirst = indexFirst;
                                        counterSecond += increaseSecond;
                                    } else if (firstLength < secondLength) {
                                        counterFirst = indexFirst;
                                        counterSecond += increaseSecond; // increase the smaller one, as the larger one skipped more already
                                    } else if (firstLength > secondLength) {
                                        counterSecond = indexSecond;
                                        counterFirst += increaseFirst;
                                    } else { // everything equal, just increase the first
                                        counterFirst = indexFirst;
                                        counterSecond += increaseSecond;
                                    } // end else
                                } // end else
                            } // end else
                        } // end else
                    } while (counterFirst < firstLength && counterSecond < secondLength);
                } // end for

                int maxSequenced = -1, maxMatched = -1;
                for (int i = 0; i < numberOfIterations; i++) {
                    if (sequencedChars[i] > maxSequenced || (sequencedChars[i] == maxSequenced && matchedChars[i] > maxMatched)) {
                        maxSequenced = sequencedChars[i];
                        maxMatched = matchedChars[i];
                    }
                } // end for

                if (maxMatched > 0) {
                    double unMatchedProportionFirst = (double) (firstLength - maxMatched) / firstLength, unMatchedProportionSecond = (double) (secondLength - maxMatched) / secondLength; // casting is important, else error

                    // applied to the shorter string
                    final int minLength = Math.min(firstLength, secondLength);
                    final int maxLength = Math.max(firstLength, secondLength);

                    final double minimumChanceNot = Math.abs(Math.sqrt(firstLength) - Math.sqrt(secondLength)) / maxLength;

                    if (firstLength < secondLength) {
                        unMatchedProportionFirst = unMatchedProportionFirst * (1 - minimumChanceNot) + minimumChanceNot;
                    } else if (firstLength > secondLength) {
                        unMatchedProportionSecond = unMatchedProportionSecond * (1 - minimumChanceNot) + minimumChanceNot;
                    } else { // they're equal, don't change anything
                    }

                    double chanceNot = 1d;
                    chanceNot /= StrictMath.pow(4d * maxLength / (4 * maxLength - 1), maxMatched);
                    chanceNot /= StrictMath.pow(2d * minLength / (2 * minLength - 1), maxSequenced); // higher importance for sequenced chars
                    chanceNot *= StrictMath.pow(unMatchedProportionFirst * unMatchedProportionSecond, .5d); // sqrt to lessen the effect on chanceNot

                    result = 1d - chanceNot;

                    // logger.info("{} {} {} {} {} {} {} {} {} {}", firstLength, secondLength, matchedChars[0], matchedChars[1], sequencedChars[0], sequencedChars[1],
                    //         unMatchedProportionFirst, unMatchedProportionSecond, minimumChanceNot, chanceNot);
                } else { // not matched a single char
                    result = 0d;
                }
            }
        }

        return result;
    }

    public static int getCollectionCapacity(final Collection<?> collection) { // returns a capacity that will hold that collection
        return getCollectionCapacity(collection, 0.75f);
    }

    public static int getCollectionCapacity(final Collection<?> collection, final float loadFactor) {
        final int size = collection == null ? 0 : collection.size();
        return getCollectionCapacity(size, loadFactor);
    }

    public static int getCollectionCapacity(final int size) {
        return getCollectionCapacity(size, 0.75f);
    }

    public static int getCollectionCapacity(final int size, final float loadFactor) {
        //noinspection NumericCastThatLosesPrecision
        return (int) ceilingPowerOf(2, size / loadFactor);
    }

    // public static void printStackTraces ()
    // {
    //     printStackTraces (System.out); // intentionally printed to System.out, as this doesn't represent an error, it's just used for debugging
    // }
    @SuppressWarnings({"ImplicitDefaultCharsetUsage", "OverlyBroadCatchBlock"})
    public static void printStackTraces(final String fileName) {
        FileOutputStream stackFileOutputStream = null;
        PrintStream stackPrintStream = null;

        try {
            stackFileOutputStream = new FileOutputStream(fileName);
            stackPrintStream = new PrintStream(stackFileOutputStream);

            printStackTraces(stackPrintStream);
        } catch (Throwable throwable) {
            logger.error("STRANGE ERROR inside printStackTraces: {}", fileName, throwable);
        } finally {
            //noinspection ConstantConditions
            closeObjects(stackPrintStream, stackFileOutputStream);
        }
    }

    public static void printStackTraces(@NotNull final PrintStream printStream) {
        printStream.println("Printing stack traces for all threads:");
        final Map<Thread, StackTraceElement[]> stacksMap = Thread.getAllStackTraces();

        for (final Entry<Thread, StackTraceElement[]> entry : stacksMap.entrySet()) {
            final Thread thread = entry.getKey();
            printStream.println(thread + " " + thread.getState() + " isDaemon=" + thread.isDaemon());
            printStackTrace(entry.getValue(), printStream);
        }
    }

    // public static void printStackTrace(StackTraceElement[] stackTraceElementsArray) {
    //     printStackTrace(stackTraceElementsArray, System.out); // intentionally printed to System.out, as this doesn't represent an error, it's just used for debugging
    // }
    @SuppressWarnings("ImplicitDefaultCharsetUsage")
    public static void printStackTrace(final StackTraceElement[] stackTraceElementsArray, final String fileName) {
        FileOutputStream stackFileOutputStream = null;
        PrintStream stackPrintStream = null;

        try {
            stackFileOutputStream = new FileOutputStream(fileName);
            stackPrintStream = new PrintStream(stackFileOutputStream);

            printStackTrace(stackTraceElementsArray, stackPrintStream);
        } catch (FileNotFoundException fileNotFoundException) {
            logger.error("STRANGE fileNotFoundException inside printStackTraces: {}", fileName, fileNotFoundException);
            // } catch (Throwable throwable) {
            //     logger.error("STRANGE ERROR inside printStackTraces: {}", fileName, throwable);
        } finally {
            //noinspection ConstantConditions
            closeObjects(stackPrintStream, stackFileOutputStream);
        }
    }

    @SuppressWarnings("ThrowableInstanceNotThrown")
    public static void printStackTrace(final StackTraceElement[] stackTraceElementsArray, final PrintStream printStream) {
        if (stackTraceElementsArray != null && printStream != null) {
            final Throwable throwable = new Throwable();
            throwable.setStackTrace(stackTraceElementsArray);
            throwable.printStackTrace(printStream);
        }
    }

    @NotNull
    public static boolean[] closeObjects(@NotNull final Object... objects) {
        final boolean[] closeSuccess = new boolean[objects.length];
        final int arrayLength = objects.length;
        for (int i = 0; i < arrayLength; i++) {
            closeSuccess[i] = closeObject(objects[i]);
        }

        return closeSuccess;
    }

    @Nullable
    public static Method getMethod(final Class<?> objectClass, final String methodName, final Class<?>... parameterTypes) {
        @Nullable final Method result;
        if (objectClass == null || methodName == null) {
            logger.error("null parameters in getMethod: {} {} {}", objectClass, methodName, objectToString(parameterTypes));
            result = null;
        } else {
            Method method = null;
            try {
                method = objectClass.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException noSuchMethodException) {
                try {
                    method = objectClass.getMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException | SecurityException e) {
                    logger.error("Exception in getMethod inner: {} {} {}", objectClass, methodName, objectToString(parameterTypes), e);
                }
            } catch (SecurityException e) {
                logger.error("securityException in getMethod: {} {} {}", objectClass, methodName, objectToString(parameterTypes), e);
            }
            result = method;
        }
        return result;
    }

    @Nullable
    public static <T> Constructor<T> getConstructor(final Class<T> objectClass, final Class<?>... parameterTypes) {
        @Nullable final Constructor<T> result;
        if (objectClass == null) {
            logger.error("null class in getConstructor: {} {}", objectClass, objectToString(parameterTypes));
            result = null;
        } else {
            Constructor<T> constructor = null;
            try {
                constructor = objectClass.getDeclaredConstructor(parameterTypes);
            } catch (NoSuchMethodException noSuchMethodException) {
                try {
                    constructor = objectClass.getConstructor(parameterTypes);
                } catch (NoSuchMethodException | SecurityException e) {
                    logger.error("exception in getConstructor inner: {} {}", objectClass, objectToString(parameterTypes), e);
                }
            } catch (SecurityException e) {
                logger.error("securityException in getConstructor: {} {}", objectClass, objectToString(parameterTypes), e);
            }
            result = constructor;
        }
        return result;
    }

    @SuppressWarnings("NestedTryStatement")
    public static boolean setSoLinger(final Object object) {
        boolean setSoLingerSuccess;

        if (object != null) {
            Class<?> objectClass = null;

            try {
                objectClass = object.getClass();
                Method objectSetSoLingerMethod = null;

                final String LINGER = "setSoLinger";
                try {
                    objectSetSoLingerMethod = objectClass.getDeclaredMethod(LINGER, boolean.class, int.class);
                } catch (NoSuchMethodException noSuchMethodException) {
                    try {
                        objectSetSoLingerMethod = objectClass.getMethod(LINGER, boolean.class, int.class);
                    } catch (NoSuchMethodException innerNoSuchMethodException) { // no setSoLinger method exists, no output required
                    } catch (SecurityException securityException) {
                        logger.error("Exception in setSoLinger inner: {} {}", objectClass, objectToString(object), securityException);
                    } // catch (Throwable throwable) {
                    //     logger.error("Exception in setSoLinger inner: {} {}", objectClass, objectToString(object), throwable);
                    // }
                } catch (SecurityException securityException) {
                    logger.error("securityException in setSoLinger: {} {}", objectClass, objectToString(object), securityException);
                } // catch (Throwable throwable) {
                //     logger.error("Throwable in setSoLinger: {} {}", objectClass, objectToString(object), throwable);
                // }
                try {
                    if (objectSetSoLingerMethod != null) {
                        if (objectSetSoLingerMethod.canAccess(object)) { // already accessible, nothing to be done
                        } else {
                            objectSetSoLingerMethod.setAccessible(true);
                        }
                        setSoLingerSuccess = true;
                        objectSetSoLingerMethod.invoke(object, true, 0);
                    } else {
                        setSoLingerSuccess = false;
                    }
                } catch (java.lang.reflect.InvocationTargetException invocationTargetException) {
                    setSoLingerSuccess = false;
                    final Throwable exceptionCause = invocationTargetException.getCause();

                    if (exceptionCause instanceof java.net.SocketException) {
                        // often setSoLinger is invoked on a closed socket, causing an exception, no output required
                    } else {
                        logger.error("Exception in setSoLinger invoke getCause: {}", new Object[]{objectClass, objectToString(object)}, invocationTargetException);
                        logger.error("Exception in setSoLinger invoke getCause (exceptionCause)", exceptionCause);
                    }
                } catch (SecurityException | IllegalAccessException | IllegalArgumentException exception) {
                    setSoLingerSuccess = false;
                    logger.error("exception in setSoLinger invoke: {} {}", objectClass, objectToString(object), exception);
                } // catch (Throwable throwable) {
                //     setSoLingerSuccess = false;
                //     logger.error("Throwable in setSoLinger invoke: {} {}", objectClass, objectToString(object), throwable);
                // }
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception exception) {
                setSoLingerSuccess = false;
                logger.error("STRANGE exception inside setSoLinger: {} {}", objectClass, objectToString(object), exception);
            } // catch (Throwable throwable) {
            //     setSoLingerSuccess = false;
            //     logger.error("STRANGE ERROR inside setSoLinger: {} {}", objectClass, objectToString(object), throwable);
            // }
        } else {
            setSoLingerSuccess = false;
        }

        return setSoLingerSuccess;
    }

    @SuppressWarnings("NestedTryStatement")
    public static boolean closeObject(final Object object) {
        // closes an object and catches all exceptions
        // the object.setSoLinger (true, 0) is invoked if a setSoLinger method exists
        setSoLinger(object);

        boolean closeSuccess;

        if (object != null) {
            Class<?> objectClass = null;

            try {
                objectClass = object.getClass();

                // Method objectFlushMethod = null;
                // try {
                //     objectFlushMethod = objectClass.getDeclaredMethod ("flush");
                // } catch (java.lang.NoSuchMethodException noSuchMethodException) {
                //     try {
                //         objectFlushMethod = objectClass.getMethod ("flush");
                //     } catch (java.lang.NoSuchMethodException innerNoSuchMethodException) {
                //         // no flush method exists, no output required
                //     } catch (Exception exception) {
                //         System.err.println ("Exception in closeObject flush inner: " + exception + " " + objectClass + " " + objectToString (object));
                //         exception.printStackTrace();
                //     }
                // } catch (Exception exception) {
                //     System.err.println ("Exception in closeObject flush: " + exception + " " + objectClass + " " + objectToString (object));
                //     exception.printStackTrace();
                // }
                // try {
                //     if (objectFlushMethod != null) {
                //         if (!objectFlushMethod.isAccessible())
                //             objectFlushMethod.setAccessible (true);
                //         objectFlushMethod.invoke (object);
                //     }
                // } catch (Exception exception) {
                //     System.err.println ("STRANGE ERROR inside closeObject() flush: " + exception + " " + objectClass);
                //     exception.printStackTrace();
                // }
                Method objectCloseMethod = null;
                final String CLOSE = "close";
                try {
                    objectCloseMethod = objectClass.getDeclaredMethod(CLOSE);
                } catch (java.lang.NoSuchMethodException noSuchMethodException) {
                    try {
                        objectCloseMethod = objectClass.getMethod(CLOSE);
                    } catch (NoSuchMethodException | SecurityException exception) {
                        logger.error("STRANGE ERROR inside closeObject() close inner: {}", new Object[]{objectClass, objectToString(object)}, exception);
                    }
                } catch (SecurityException securityException) {
                    logger.error("securityException in closeObject close: {} {}", objectClass, objectToString(object), securityException);
                } // catch (Throwable throwable) {
                //     logger.error("Exception in closeObject close: {} {}", objectClass, objectToString(object), throwable);
                // }

                try {
                    if (objectCloseMethod != null) {
                        if (!objectCloseMethod.canAccess(object)) {
                            objectCloseMethod.setAccessible(true);
                        }
                        objectCloseMethod.invoke(object);
                        closeSuccess = true;
                    } else {
                        closeSuccess = false;
                    }
                } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
                    logger.error("STRANGE ERROR inside closeObject() close: {}", new Object[]{objectClass, objectToString(object)}, exception);

                    closeSuccess = false;
                }
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception exception) {
                logger.error("STRANGE exception inside closeObject: {} {}", objectClass, objectToString(object), exception);
                closeSuccess = false;
            } // catch (Throwable throwable) {
            //     logger.error("STRANGE ERROR inside closeObject: {} {}", objectClass, objectToString(object), throwable);
            //     closeSuccess = false;
            // }
        } else {
            closeSuccess = false;
        }

        return closeSuccess;
    }

    @NotNull
    public static byte[] concatByte(final byte[] a, final int startIndexA, final int endIndexA, final byte[] b, final int startIndexB, final int endIndexB) {
        // endIndexA & endIndexB are excluded
        final int localStartIndexA, localStartIndexB, localEndIndexA, localEndIndexB;

        localStartIndexA = Math.max(startIndexA, 0);
        localEndIndexA = Math.max(endIndexA, 0);
        localStartIndexB = Math.max(startIndexB, 0);
        localEndIndexB = Math.max(endIndexB, 0);

        final byte[] resultArray = new byte[localEndIndexA - localStartIndexA + localEndIndexB - localStartIndexB];
        System.arraycopy(a, localStartIndexA, resultArray, 0, localEndIndexA - localStartIndexA);
        System.arraycopy(b, localStartIndexB, resultArray, localEndIndexA - localStartIndexA, localEndIndexB - localStartIndexB);

        return resultArray;
    }

    @SuppressWarnings({"HardcodedLineSeparator", "MagicCharacter"})
    public static String encryptString(final String initialString, final int encryptKey) {
        @Nullable final String result;

        if (initialString != null) {
            final char[] tempCharArray = initialString.toCharArray();
            final int tempInt = tempCharArray.length;

            for (int i = 0; i < tempInt; i++) {
                if (tempCharArray[i] != '\r' && tempCharArray[i] != '\n') {
                    //noinspection NumericCastThatLosesPrecision,ImplicitNumericConversion
                    tempCharArray[i] += (char) encryptKey;
                }
            }
            result = new String(tempCharArray);
        } else {
            result = null;
        }

        return result;
    }

    public static boolean encryptFile(final String fileName, final int encryptKey) {
        // encrypts and replaces a file
        boolean success = false;

        if (new File(fileName).exists()) {
            SynchronizedReader synchronizedReader = null;
            SynchronizedWriter synchronizedWriter = null;
            String fileLine = null;
            final String tempFileName = tempFileName(fileName);

            try {
                synchronizedReader = new SynchronizedReader(fileName);
                synchronizedWriter = new SynchronizedWriter(tempFileName, false);

                fileLine = synchronizedReader.readLine();
                while (fileLine != null) {
                    synchronizedWriter.write(encryptString(fileLine, encryptKey) + "\r\n");
                    fileLine = synchronizedReader.readLine();
                } // end while
                synchronizedWriter.flush();
                success = true;
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException iOException) {
                logger.error("STRANGE ERROR inside encryptFile: {} {}", new Object[]{fileName, fileLine}, iOException);
            } finally {
                //noinspection ConstantConditions
                closeObjects(synchronizedReader, synchronizedWriter);
            }

            try {
                //noinspection ResultOfMethodCallIgnored
                new File(fileName).delete();
                //noinspection ResultOfMethodCallIgnored
                new File(tempFileName).renameTo(new File(fileName));
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception exception) {
                logger.error("STRANGE exception inside readInputFile() File.delete: {} {}", fileName, tempFileName, exception);
            }
        } else {
            logger.error("STRANGE file doesn't exist in encryptFile, fileName={}", fileName);
        }

        return success;
    }

    @NotNull
    public static String tempFileName(final String fileName) {
        int nameTrailer = -1;
        final long time = System.currentTimeMillis();

        try {
            final SecureRandom generator = new SecureRandom();
            nameTrailer = generator.nextInt();

            while (new File(fileName + time + "." + nameTrailer).exists()) {
                nameTrailer = generator.nextInt();
            }
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception exception) {
            logger.error("STRANGE ERROR inside tempFileNameRandom: {} {} {}", fileName, time, nameTrailer, exception);
        }

        return fileName + time + "." + nameTrailer;
    }

    public static Object readObjectFromFile(final String fileName) {
        Object object = null;
        ObjectInputStream objectInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        FileInputStream fileInputStream = null;

        if (fileName != null) {
            try {
                if (new File(fileName).exists()) {
                    fileInputStream = new FileInputStream(fileName);
                    bufferedInputStream = new BufferedInputStream(fileInputStream);
                    //noinspection resource,IOResourceOpenedButNotSafelyClosed
                    objectInputStream = new ObjectInputStream(bufferedInputStream);
                    object = objectInputStream.readObject();
                } else {
                    logger.warn("Can't read object, file {} does not exist!", fileName);
                }
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException | ClassNotFoundException exception) {
                logger.error("STRANGE ERROR inside readObjectFromFile: {}", fileName, exception);
            } finally {
                //noinspection ConstantConditions
                closeObjects(objectInputStream, bufferedInputStream, fileInputStream);
            }
        } else {
            logger.error("STRANGE fileName null in readObjectFromFile, timeStamp={}", System.currentTimeMillis());
        }

        return object;
    }

    public static void synchronizedWriteObjectToFile(final Serializable object, final String fileName) {
        synchronizedWriteObjectToFile(object, fileName, false);
    }

    public static void synchronizedWriteObjectToFile(@NotNull final Serializable object, final String fileName, final boolean appendFile) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (object) {
            writeObjectToFile(object, fileName, appendFile);
        }
    }

    public static void synchronizedWriteObjectsToFiles(@NotNull final Map<Serializable, String> fileNamesMap) {
        // should not be used if objects in keySet are collections; strange behaviour results
        for (final Entry<Serializable, String> entry : fileNamesMap.entrySet()) {
            synchronizedWriteObjectToFile(entry.getKey(), entry.getValue());
        }
    }

    public static void writeObjectToFile(final Serializable object, final String fileName) {
        writeObjectToFile(object, fileName, false);
    }

    public static void writeObjectToFile(final Serializable object, final String fileName, final boolean appendFile) {
        ObjectOutputStream objectOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(fileName, appendFile);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            //noinspection resource,IOResourceOpenedButNotSafelyClosed
            objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
            objectOutputStream.writeObject(object);
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException iOException) {
            logger.error("STRANGE iOException inside writeObjectToFile: {}", fileName, iOException);
            // } catch (Throwable throwable) {
            //     logger.error("STRANGE ERROR inside writeObjectToFile: {}", fileName, throwable);
        } finally {
            //noinspection ConstantConditions
            closeObjects(objectOutputStream, bufferedOutputStream, fileOutputStream);
        }
    }

    public static void writeObjectsToFiles(@NotNull final Map<Serializable, String> fileNamesMap) {
        // should not be used if objects in keySet are collections; strange behaviour results
        for (final Entry<Serializable, String> entry : fileNamesMap.entrySet()) {
            writeObjectToFile(entry.getKey(), entry.getValue());
        }
    }

    @NotNull
    public static String getHexString(@NotNull final byte[] b) {
        // convert a byte array to a Hex string
        final StringBuilder result = new StringBuilder(b.length);
        for (final byte value : b) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }

        return result.toString();
    }

    public static String backwardWordsString(final String initialString) {
        // returns the initialString with words ordered in reverse; words are bordered by spaces
        @Nullable final String result;

        if (initialString != null) {
            @SuppressWarnings("UseOfStringTokenizer") final StringTokenizer stringTokenizer = new StringTokenizer(initialString, " ", true);
            final Stack<String> stack = new Stack<>();
            final StringBuilder stringBuilder = new StringBuilder(initialString.length());
            while (stringTokenizer.hasMoreElements()) {
                final String word = stringTokenizer.nextToken();
                if (!word.isEmpty()) {
                    stack.push(word);
                }
            }
            while (!stack.isEmpty()) {
                stringBuilder.append(stack.pop());
            }

            result = stringBuilder.toString();
        } else {
            logger.error("STRANGE initialString null in backwardWordsString, timeStamp={}", System.currentTimeMillis());
            result = null;
        }

        return result;
    }

    public static String backwardString(final String initialString) {
        // returns the initialString in reverse
        @Nullable final String result;

        if (initialString != null) {
            final char[] charArray = initialString.toCharArray();
            final int arrayLength = charArray.length;

            for (int i = 0; i < arrayLength; i++) {
                charArray[i] = initialString.charAt(arrayLength - 1 - i);
            }
            result = new String(charArray);
        } else {
            logger.error("STRANGE initialString null in backwardString, timeStamp={}", System.currentTimeMillis());
            result = null;
        }

        return result;
    }

    @SuppressWarnings("CharacterComparison")
    @NotNull
    public static String trimIP(@NotNull final String IP) {
        // removes heading zeros from IP values
        boolean isIP = true;
        int numberOfDots = 0;
        StringBuilder resultStringBuilder = new StringBuilder(IP);

        try {
            final int length = IP.length();
            for (int i = 0; i < length; i++) {
                if ((IP.charAt(i) < '0' || IP.charAt(i) > '9') && IP.charAt(i) != '.') {
                    isIP = false;
                    break;
                }
                if (IP.charAt(i) == '.') {
                    numberOfDots++;
                    if (numberOfDots > 3) {
                        break;
                    }
                }
            }
            if (isIP && numberOfDots == 3) {
                final int secondPointIndex = IP.indexOf('.', IP.indexOf('.') + ".".length());
                final int afterSecondPointIndex = secondPointIndex + ".".length();

                final int IP1, IP2, IP3, IP4;
                IP1 = Integer.parseInt(IP.substring(0, IP.indexOf('.')));
                IP2 = Integer.parseInt(IP.substring(IP.indexOf('.') + ".".length(), secondPointIndex));
                IP3 = Integer.parseInt(IP.substring(afterSecondPointIndex, IP.indexOf('.', afterSecondPointIndex)));
                IP4 = Integer.parseInt(IP.substring(IP.indexOf('.', afterSecondPointIndex) + ".".length()));
                resultStringBuilder = new StringBuilder(String.valueOf(IP1)).append('.').append(IP2).append('.').append(IP3).append('.').append(IP4);
            } else {
                logger.error("not IP in trimIP: {}", IP);
            }
        } catch (NumberFormatException numberFormatException) {
            logger.error("STRANGE numberFormatException inside trimIP: {}", IP, numberFormatException);
        } // catch (Throwable throwable) {
        //     logger.error("STRANGE ERROR inside trimIP: {}", IP, throwable);
        // }

        return resultStringBuilder.toString();
    }

    public static boolean goodPort(final String tempPort) {
        boolean isGood = false;
        int port;

        try {
            port = Integer.parseInt(tempPort);
            if (port > 65535) {
                port %= 65536;
            }
            if (port >= 1) {
                isGood = true;
            }
        } catch (NumberFormatException numberFormatException) { // isGood == false
        } // catch (Throwable throwable) {
        //     logger.error("STRANGE ERROR inside goodPort: {}", tempPort, throwable);
        //     isGood = false;
        // }

        return isGood;
    }

    @SuppressWarnings({"OverlyNestedMethod", "OverlyComplexMethod", "OverlyLongMethod", "CharacterComparison"})
    public static boolean goodDomain(final String host) {
        boolean isGood = false;

        if (host == null || !isPureAscii(host) || host.indexOf('.') <= 0 || host.lastIndexOf('.') >= host.length() - 1) { // isGood already false, nothing to be done
        } else {
            String modifiedHost = URLDecoder.decode(host, StandardCharsets.UTF_8);

            if (modifiedHost.contains(File.separator) || modifiedHost.indexOf('?') >= 0 || modifiedHost.indexOf(' ') >= 0) { // isGood already false, nothing to be done
            } else {
                try {
                    modifiedHost = IDN.toASCII(modifiedHost, IDN.ALLOW_UNASSIGNED);
                    // } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
                } catch (IllegalArgumentException illegalArgumentException) {
                    logger.error("STRANGE ERROR inside goodDomain() IDN.toASCII: {}", modifiedHost, illegalArgumentException);
                }

                modifiedHost = modifiedHost.toLowerCase(Locale.ENGLISH).trim();
                isGood = true;
                final int length = modifiedHost.length();
                for (int i = 0; i < length; i++) {
                    if ((modifiedHost.charAt(i) < '0' || modifiedHost.charAt(i) > '9') && (modifiedHost.charAt(i) < 'a' || modifiedHost.charAt(i) > 'z') &&
                        modifiedHost.charAt(i) != '.' && modifiedHost.charAt(i) != '-' && modifiedHost.charAt(i) != '_') {
                        // '_' is not standard but it is sometimes, rarely, used
                        isGood = false;
                        break;
                    }
                }

                if (isGood) {
                    boolean isIP = true;
                    int numberOfDots = 0;
                    for (int i = 0; i < length; i++) {
                        if ((modifiedHost.charAt(i) < '0' || modifiedHost.charAt(i) > '9') && modifiedHost.charAt(i) != '.') {
                            isIP = false;
                        }
                        if (modifiedHost.charAt(i) == '.') {
                            numberOfDots++;
                        }
                    }

                    if (isIP) {
                        if (numberOfDots == 3) {
                            try {
                                final int secondPointIndex = modifiedHost.indexOf('.', modifiedHost.indexOf('.') + ".".length());
                                final int afterSecondPointIndex = secondPointIndex + ".".length();

                                final int IP1, IP2, IP3, IP4;
                                IP1 = Integer.parseInt(modifiedHost.substring(0, modifiedHost.indexOf('.')));
                                IP2 = Integer.parseInt(modifiedHost.substring(modifiedHost.indexOf('.') + ".".length(), secondPointIndex));
                                IP3 = Integer.parseInt(modifiedHost.substring(afterSecondPointIndex, modifiedHost.indexOf('.', afterSecondPointIndex)));
                                IP4 = Integer.parseInt(modifiedHost.substring(modifiedHost.indexOf('.', afterSecondPointIndex) + ".".length()));
                                if (IP1 <= 0 || IP1 >= 224 || IP1 == 7 || IP1 == 10 || IP1 == 127 || IP2 < 0 || IP2 > 255 || IP3 < 0 || IP3 > 255 || IP4 < 0 || IP4 > 255) {
                                    isGood = false;
                                } else if (IP1 == 169 && IP2 == 254) {
                                    isGood = false;
                                } else if (IP1 == 192 && IP2 == 168) {
                                    isGood = false;
                                } else if (IP1 == 172 && IP2 >= 16 && IP2 <= 31) {
                                    isGood = false;
                                } else if (IP1 == 203 && IP2 == 0 && IP3 == 113) {
                                    isGood = false;
                                } else if (IP1 == 198 && IP2 == 51 && IP3 == 100) {
                                    isGood = false;
                                } else if (IP1 == 192 && IP2 == 88 && IP3 == 99) {
                                    isGood = false;
                                } else if (IP1 == 192 && IP2 == 0 && IP3 == 0) {
                                    isGood = false;
                                } else if (IP1 == 192 && IP2 == 0 && IP3 == 2) {
                                    isGood = false;
                                } else if (IP1 == 198 && IP2 >= 18 && IP2 <= 19) {
                                    isGood = false;
                                } else { // isGood is true, nothing to be done
                                }
                                // last updated 30-12-2013 from http://en.wikipedia.org/wiki/Reserved_IP_addresses#Reserved_IPv4_addresses and
                                //     http://www.iana.org/assignments/ipv4-address-space/
                                // (only reserved ranges were taken out, not the unallocated ones)
                            } catch (NumberFormatException numberFormatException) {
                                logger.error("NumberFormatException inside goodDomain() isIP: {}", modifiedHost, numberFormatException);
                                isGood = false;
                            } // catch (Throwable throwable) {
                            //     logger.error("STRANGE ERROR inside goodDomain isIP: {}", modifiedHost, throwable);
                            //     isGood = false;
                            // }
                        } else {
                            isGood = false;
                        }
                    } else {
                        if (modifiedHost.length() > 253) {
                            isGood = false;
                        } else {
                            int pointIndex1 = 0, pointIndex2;

                            pointIndex2 = modifiedHost.indexOf('.', pointIndex1 + 1);
                            while (pointIndex2 > 0) {
                                if (pointIndex2 - pointIndex1 > 63) {
                                    isGood = false;
                                    break;
                                }
                                pointIndex1 = pointIndex2;

                                pointIndex2 = modifiedHost.indexOf('.', pointIndex1 + 1);
                            } // end while

                            if (isGood) {
                                if (modifiedHost.length() - pointIndex1 > 63) {
                                    isGood = false;
                                } else {

                                    final String lastLabel = modifiedHost.substring(modifiedHost.lastIndexOf('.') + ".".length()).toUpperCase(Locale.ENGLISH);
                                    if (lastLabel.isEmpty() || !TLDs.contains(lastLabel)) {
                                        isGood = false;
                                    } else { // it seems I reached the end of checking and isGood is true
                                    }
                                }
                            } else { // isGood already false, nothing to be done
                            }
                        }
                    }
                } else { // isGood already false, nothing to be done
                }
            }
        }
        return isGood;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static String getUserAgent() {
        String userAgent = "Mozilla/";

        try {
            final SecureRandom secureRandom = new SecureRandom();
            switch (secureRandom.nextInt(3)) {
                case 0:
                    userAgent += "4.0 (compatible; MSIE 7.0; ";
                    break;
                case 1:
                    userAgent += "4.0 (compatible; MSIE 8.0; ";
                    break;
                case 2:
                    userAgent += "5.0 (Windows; U; ";
                    break;
            }
            switch (secureRandom.nextInt(7)) {
                case 0:
                    userAgent += "Windows NT 6.0";
                    break;
                case 1:
                    userAgent += "Windows NT 5.2";
                    break;
                case 2:
                    userAgent += "Windows NT 5.1";
                    break;
                case 3:
                    userAgent += "Windows NT 5.01";
                    break;
                case 4:
                    userAgent += "Windows NT 5.0";
                    break;
                case 5:
                    userAgent += "Windows NT 4.0";
                    break;
                case 6:
                    userAgent += "Windows 98";
                    break;
            }
            userAgent += ")";
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception exception) {
            logger.error("STRANGE ERROR inside getUserAgent: {}", userAgent, exception);
        }

        return userAgent;
    }

    @SuppressWarnings("CallToSuspiciousStringMethod")
    public static byte getSocksType(@NotNull final String proxyType) {
        final byte socksType;

        if ("socks4".equalsIgnoreCase(proxyType)) {
            socksType = 4;
        } else if ("socks5".equalsIgnoreCase(proxyType)) {
            socksType = 5;
        } else {
            socksType = 5;
            logger.warn("Strange socks proxy type: {}", proxyType);
        }

        return socksType;
    }

    public static String linkRemoveProtocol(final String link) {
        final String result;
        @SuppressWarnings("HardcodedFileSeparator") final String SEPARATOR = "://";

        result = link != null && link.contains(SEPARATOR) ? new String(link.substring(link.indexOf(SEPARATOR) + SEPARATOR.length())) : link;

        return result;
    }

    public static String linkRemovePort(final String link) {
        @Nullable String result;
        @SuppressWarnings("HardcodedFileSeparator") final String SEPARATOR = "://";

        if (link != null) {
            String modifiedLink = link;

            if (modifiedLink.contains(SEPARATOR)) {
                final int afterSeparatorIndex = modifiedLink.indexOf(SEPARATOR) + SEPARATOR.length();
                result = modifiedLink.substring(0, afterSeparatorIndex);
                modifiedLink = modifiedLink.substring(afterSeparatorIndex);
            } else {
                result = "";
            }

            @SuppressWarnings("HardcodedFileSeparator") final char urlSeparator = '/';
            final boolean noSlashAndColonBeforeQuestionMark = modifiedLink.indexOf(urlSeparator) < 0 && modifiedLink.indexOf(':') < modifiedLink.indexOf('?');
            final boolean colonBeforeSeparator = modifiedLink.indexOf(':') < modifiedLink.indexOf(urlSeparator);
            if (modifiedLink.indexOf(':') >= 0 && (colonBeforeSeparator || noSlashAndColonBeforeQuestionMark || (modifiedLink.indexOf(urlSeparator) < 0 && modifiedLink.indexOf('?') < 0))) {
                if (colonBeforeSeparator) {
                    result += modifiedLink.substring(0, modifiedLink.indexOf(':')) + modifiedLink.substring(modifiedLink.indexOf(urlSeparator));
                } else if (noSlashAndColonBeforeQuestionMark) {
                    result += modifiedLink.substring(0, modifiedLink.indexOf(':')) + urlSeparator + modifiedLink.substring(modifiedLink.indexOf('?'));
                } else {
                    // the case when: modifiedLink.indexOf ("/") < 0 && modifiedLink.indexOf ("?") < 0
                    result += modifiedLink.substring(0, modifiedLink.indexOf(':')) + urlSeparator;
                }
            } else {
                result += modifiedLink;
            }
        } else {
            result = null;
        }

        return result;
    }

    public static String linkRemoveQuery(final String link) {
        final String result;

        result = link != null && link.indexOf('?') >= 0 ? new String(link.substring(0, link.indexOf('?'))) : link;

        return result;
    }

    public static String getLinkHost(final String link) {
        @Nullable String result;
        String modifiedLink = link;
        @SuppressWarnings("HardcodedFileSeparator") final char urlSeparator = '/';
        modifiedLink = linkRemoveProtocol(modifiedLink);
        modifiedLink = linkRemovePort(modifiedLink);
        modifiedLink = linkRemoveQuery(modifiedLink);

        result = modifiedLink != null && modifiedLink.indexOf(urlSeparator) >= 0 ? new String(modifiedLink.substring(0, modifiedLink.indexOf(urlSeparator))) : modifiedLink;

        if (goodDomain(result)) {
            result = result != null ? result.toLowerCase(Locale.ENGLISH) : null;
        } else {
            result = null;
        }

        return result;
    }

    public static boolean linkMatches(final String path, final String checkedLink) {
        // all sub-domains are taken
        // if path is a folder, only links to that folder and sub-folders are taken
        // if path is a particular page, only that page is taken
        final boolean linkMatches;
        final String checkedHost;
        String modifiedLink = checkedLink;
        String modifiedPath = path;
        @SuppressWarnings("HardcodedFileSeparator") final char urlSeparator = '/';

        // no support for protocol, so protocol is removed
        modifiedLink = linkRemoveProtocol(modifiedLink);
        modifiedPath = linkRemoveProtocol(modifiedPath);

        // no support for port, so port is removed
        modifiedLink = linkRemovePort(modifiedLink);
        modifiedPath = linkRemovePort(modifiedPath);

        // no support for query, so query is removed
        modifiedLink = linkRemoveQuery(modifiedLink);
        modifiedPath = linkRemoveQuery(modifiedPath);

        checkedHost = getLinkHost(modifiedLink);

        if (checkedHost != null) {
            modifiedLink = modifiedLink.indexOf(urlSeparator) >= 0 ? checkedHost + modifiedLink.substring(modifiedLink.indexOf(urlSeparator)) : checkedHost + urlSeparator;

            if (goodDomain(modifiedPath)) {
                modifiedPath = modifiedPath.toLowerCase(Locale.ENGLISH);

                // check if link is on same domain or a sub-domain
                linkMatches = checkedHost.equalsIgnoreCase(modifiedPath) || checkedHost.endsWith("." + modifiedPath);
            } else {
                final String substring = modifiedPath.substring(0, modifiedPath.indexOf(urlSeparator));
                if (modifiedPath.indexOf(urlSeparator) >= 0 && goodDomain(substring)) {
                    modifiedPath = substring.toLowerCase(Locale.ENGLISH) + modifiedPath.substring(modifiedPath.indexOf(urlSeparator));

                    // path is folder, check if link is in the same folder or sub-folders (same domain as well)
                    // path is a particular page and has to be identical with link in this case
                    linkMatches = !modifiedPath.isEmpty() && modifiedPath.charAt(modifiedPath.length() - 1) == urlSeparator ? modifiedLink.startsWith(modifiedPath) : modifiedLink.equals(modifiedPath);
                } else {
                    logger.warn("Bogus path in linkMatches: {}", path);
                    linkMatches = false;
                }
            }
        } else {
            linkMatches = false;

            //noinspection SpellCheckingInspection
            if (modifiedLink.startsWith("XXXXXXXXXXX") || modifiedLink.startsWith("static.+") || modifiedLink.startsWith("%2568") || modifiedLink.startsWith("%01%25")) {
                // known bogus or unavailable links, no need to fill logs with trash
            } else {
                logger.warn("Bogus modifiedLink in linkMatches: {}", modifiedLink);
            }
        }

        return linkMatches;
    }

    // public static String hexToAscii (String host)
    // {
    //     // returns the unmodified host in case of error, or if host.indexOf ("%") < 0
    //     // %E0%B9%81%E0%B8%AB%E0%B8%A7%E0%B8%99%E0%B9%81%E0%B8%9F%E0%B8%8A%E0%B8%B1%E0%B9%88%E0%B8%99.com
    //     String resultString;
    //     try {
    //         if (host.indexOf ("%") >= 0) {
    //             String[] hostLabelsArray = host.split ("\\.");
    //             resultString = "";
    //             for (String hostLabel : hostLabelsArray)
    //                 if (hostLabel.startsWith ("%")) {
    //                 String[] hexChunksArray = hostLabel.split ("\\%");
    //                 byte[] byteArray = new byte [hexChunksArray.length];
    //                 int byteArrayIndex = 0;
    //                 for (String hexCode : hexChunksArray)
    //                     if (hexCode.length() > 0) {
    //                     byteArray [byteArrayIndex] = (byte) Integer.parseInt (hexCode, 16);
    //                     byteArrayIndex ++;
    //                 }
    //                 resultString += new String (byteArray, 0, byteArrayIndex, UTF8_CHARSET) + ".";
    //             }   else resultString += hostLabel + ".";
    //             resultString = resultString.substring (0, resultString.lastIndexOf ("."));
    //         } else resultString = host;
    //     } catch (NumberFormatException numberFormatException) {
    //         resultString = host;
    //     } catch (Exception exception) {
    //         System.err.println ("STRANGE ERROR inside hexToAscii: " + exception + " " + host);
    //         exception.printStackTrace();
    //         resultString = host;
    //     }
    //     return new String (resultString);
    // }
    //
    @NotNull
    public static String addSpaces(final Object object, final int finalSize, final boolean inFront) {
        String returnString;

        returnString = object != null ? String.valueOf(object) : "";

        final int spacesNeeded = finalSize - returnString.length();
        final String spacesString = spacesNeeded > 0 ? new String(new char[spacesNeeded]).replace('\0', ' ') : "";

        if (inFront) {
            returnString = spacesString + returnString;
        } else {
            returnString += spacesString;
        }

        return returnString;
    }

    @Contract(pure = true)
    public static String containsSubstring(final String mainString, final String[] substrings) {
        String foundSubstring = null;

        if (mainString != null && substrings != null) {
            final int length = substrings.length;
            for (int i = 0; i < length && foundSubstring == null; i++) {
                if (substrings[i] != null && mainString.contains(substrings[i])) {
                    foundSubstring = new String(substrings[i]); // will also invalidate a for condition that will cause loop exit
                }
            }
        }

        return foundSubstring;
    }

    @NotNull
    public static String convertMillisToDate(final long millis, final String timeZoneName) {
        // 12.08.2010 23:45:19.342
        final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneName));

        return dateFormat.format(new Date(millis));
    }

    @NotNull
    public static String convertMillisToDate(final long millis) {
        return convertMillisToDate(millis, "UTC");
    }

    @NotNull
    public static String getFormattedDate() {
        return getFormattedDate("UTC"); // defaults to UTC, not local time zone
    }

    @NotNull
    public static String getFormattedDate(final String timeZoneName) {
        // 12.08.2010 23:45:19.342
        final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneName));

        return dateFormat.format(new Date());
    }

    @NotNull
    public static String addCommas(final Object value) {
        return addCommas(String.valueOf(value), (byte) 3, ",", ".");
    }

    @NotNull
    public static String addCommas(final double value, final int nDecimals) {
        //noinspection StringConcatenationInFormatCall
        return addCommas(String.format("%." + nDecimals + "f", value), (byte) 3, ",", ".");
    }

    @NotNull
    public static String addCommas(@NotNull final String initialString, final byte groupSize, final String commaDelimiter, final String periodDelimiter) {
        int periodIndex = initialString.indexOf(periodDelimiter);

        if (periodIndex < 0) {
            periodIndex = initialString.length();
        }

        final int nCommas = (periodIndex - 1) / groupSize, firstCommaIndex = periodIndex - nCommas * groupSize;
        final StringBuilder resultString = new StringBuilder(initialString.substring(0, firstCommaIndex));

        for (int i = 0; i < nCommas; i++) {
            resultString.append(commaDelimiter).append(initialString.substring(firstCommaIndex + i * groupSize, firstCommaIndex + (i + 1) * groupSize));
        }
        if (periodIndex < initialString.length()) {
            resultString.append(initialString.substring(periodIndex));
        }

        return resultString.toString();
    }

    public static boolean isPureAscii(final CharSequence asciiString) {
        return StandardCharsets.US_ASCII.newEncoder().canEncode(asciiString);
    }

    @Contract(pure = true)
    public static int byteArrayIndexOf(final byte[] data, final byte[] pattern) {
        return byteArrayIndexOf(data, pattern, 0);
    }

    @Contract(pure = true)
    public static int byteArrayIndexOf(@NotNull final byte[] data, final byte[] pattern, final int beginIndex) {
        // Search the data byte array for the first occurrence of the byte array pattern.
        int returnValue = -1;
        final int[] failure = byteArrayComputeFailure(pattern);
        int j = 0;
        final int length = data.length;

        for (int i = beginIndex; i < length; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            } // end while

            if (pattern[j] == data[i]) {
                j++;
            }
            if (j == pattern.length) {
                returnValue = i - pattern.length + 1;
                break;
            }
        }

        return returnValue;
    }

    @NotNull
    @Contract(pure = true)
    public static int[] byteArrayComputeFailure(@NotNull final byte[] pattern) {
        // Computes the failure function using a boot-strapping process, where the pattern is matched against itself.
        final int length = pattern.length;
        final int[] failure = new int[length];
        int j = 0;

        for (int i = 1; i < length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }

    public static double logOfBase(final double base, final double num) {
        return StrictMath.log(num) / StrictMath.log(base);
    }

    public static double ceilingPowerOf(final double base, final double num) {
        // returns the closest higher or equal power of the base to the given num
        return StrictMath.pow(base, Math.ceil(logOfBase(base, num)));
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public static <K, V> boolean compareSortedLinkedHashMap(@NotNull final LinkedHashMap<K, V> firstMap, final LinkedHashMap<K, V> secondMap) {
        boolean areEqual;

        areEqual = firstMap.equals(secondMap);
        if (areEqual) {
            final List<Map.Entry<K, V>> firstList = new ArrayList<>(firstMap.entrySet());
            final List<Map.Entry<K, V>> secondList = new ArrayList<>(secondMap.entrySet());
            final Iterator<Map.Entry<K, V>> firstIterator = firstList.iterator();
            final Iterator<Map.Entry<K, V>> secondIterator = secondList.iterator();

            while (areEqual && firstIterator.hasNext() && secondIterator.hasNext()) {
                final Map.Entry<K, V> firstEntry = firstIterator.next();
                final Map.Entry<K, V> secondEntry = secondIterator.next();
                if (!firstEntry.getKey().equals(secondEntry.getKey()) || !firstEntry.getValue().equals(secondEntry.getValue())) {
                    areEqual = false; // no need for break, condition is checked by while
                }
            }
        } else { // areEqual already false, nothing to be done
        }

        return areEqual;
    }

    @NotNull
    public static <K, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(@NotNull final Map<K, V> map, final boolean ascendingOrder) {
        final List<Entry<K, V>> list = new ArrayList<>(map.entrySet());

        list.sort((o1, o2) -> {
            int result = (o1.getValue()).compareTo(o2.getValue());

            if (!ascendingOrder) {
                result = -result;
            }

            return result;
        });

        // logger.info (list);
        @SuppressWarnings("NumericCastThatLosesPrecision") final LinkedHashMap<K, V> result = new LinkedHashMap<>((int) ceilingPowerOf(2, map.size() / 0.75), 0.75f);

        for (final Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public static <E> E getRandomElementFromSet(@NotNull final Collection<E> collection) {
        final int setSize = collection.size(), randomPosition = new SecureRandom().nextInt(setSize);
        int counter = 0;
        E returnValue = null;

        for (final E element : collection) {
            if (counter == randomPosition) {
                returnValue = element;
                break;
            }
            counter++;
        }

        return returnValue;
    }

    @SuppressWarnings({"OverlyBroadThrowsClause", "resource", "IOResourceOpenedButNotSafelyClosed", "ChannelOpenedButNotSafelyClosed"})
    public static void copyFile(final File sourceFile, @NotNull final File destFile)
            throws java.io.IOException {
        if (!destFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            destFile.createNewFile();
        }

        FileInputStream sourceStream = null;
        FileOutputStream destinationStream = null;
        FileChannel source = null, destination = null;

        try {
            sourceStream = new FileInputStream(sourceFile);
            source = sourceStream.getChannel();
            destinationStream = new FileOutputStream(destFile);
            destination = destinationStream.getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            //noinspection ConstantConditions
            closeObjects(source, sourceStream, destination, destinationStream);
        }
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> T[] concatArrays(@NotNull final T[] firstArray, @NotNull final T[]... restArrays) {
        // because this works with generic type, it won't work with primitive types
        int totalLength = firstArray.length;

        for (final T[] array : restArrays) {
            totalLength += array.length;
        }

        final T[] resultArray = Arrays.copyOf(firstArray, totalLength);
        int offset = firstArray.length;

        for (final T[] array : restArrays) {
            System.arraycopy(array, 0, resultArray, offset, array.length);
            offset += array.length;
        }

        return resultArray;
    }

    @SuppressWarnings({"resource", "IOResourceOpenedButNotSafelyClosed"})
    public static byte[] compressByteArray(final byte[] byteArray, final String compressionFormat)
            throws IOException {
        // gzip & deflate compression formats accepted
        byte[] returnValue;
        ByteArrayOutputStream byteArrayOutputStream = null;
        GZIPOutputStream gzipOutputStream = null;
        DeflaterOutputStream deflaterOutputStream = null;

        try {
            byteArrayOutputStream = new ByteArrayOutputStream();

            final boolean knownFormat;
            if (compressionFormat != null) {
                if ("gzip".equalsIgnoreCase(compressionFormat)) {
                    gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
                    gzipOutputStream.write(byteArray);
                    gzipOutputStream.close(); // close() finishes the compression task

                    knownFormat = true;
                } else if ("deflate".equalsIgnoreCase(compressionFormat)) {
                    deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream);
                    deflaterOutputStream.write(byteArray);
                    deflaterOutputStream.close(); // close() finishes the compression task

                    knownFormat = true;
                } else {
                    knownFormat = false;
                }
            } else {
                knownFormat = false;
            }

            returnValue = knownFormat ? byteArrayOutputStream.toByteArray() : byteArray;
        } finally {
            //noinspection ConstantConditions
            closeObjects(gzipOutputStream, deflaterOutputStream, byteArrayOutputStream);
        }
        //noinspection ConstantConditions
        if (byteArrayOutputStream != null && byteArray != null && byteArray.length != 0) {
            logger.debug("compressByteArray using {} , compression ratio {}", compressionFormat, String.format("%.2f %%", (double) byteArrayOutputStream.size() / byteArray.length * 100));
        } else {
            logger.error("byteArrayOutputStream null or byteArray null or byteArray.length zero in compressByteArray: {} {}", byteArrayOutputStream, byteArray);
        }

        return returnValue;
    }

    @SuppressWarnings({"resource", "IOResourceOpenedButNotSafelyClosed"})
    public static byte[] decompressByteArray(final byte[] byteArray, final String compressionFormat)
            throws IOException {
        // gzip & deflate compression formats accepted
        byte[] returnValue;
        BufferedInputStream bufferedInputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        GZIPInputStream gZIPInputStream = null;
        InflaterInputStream inflaterInputStream = null;

        try {
            byteArrayInputStream = new ByteArrayInputStream(byteArray);
            final boolean knownFormat;

            if (compressionFormat != null) {
                if ("gzip".equalsIgnoreCase(compressionFormat)) {
                    gZIPInputStream = new GZIPInputStream(byteArrayInputStream);
                    bufferedInputStream = new BufferedInputStream(gZIPInputStream);
                    knownFormat = true;
                } else if ("deflate".equalsIgnoreCase(compressionFormat)) {
                    inflaterInputStream = new InflaterInputStream(byteArrayInputStream);
                    bufferedInputStream = new BufferedInputStream(inflaterInputStream);
                    knownFormat = true;
                } else {
                    knownFormat = false;
                }
            } else {
                knownFormat = false;
            }

            if (knownFormat) {
                byteArrayOutputStream = new ByteArrayOutputStream();
                final byte[] buffer = new byte[1024];

                @SuppressWarnings("null")
                int readLength = bufferedInputStream.read(buffer);
                while (readLength > 0) {
                    byteArrayOutputStream.write(buffer, 0, readLength);
                    readLength = bufferedInputStream.read(buffer);
                }

                returnValue = byteArrayOutputStream.toByteArray();
            } else {
                returnValue = byteArray;
            }
            // catch (Exception exception) {
            //     FileOutputStream fileOutputStream = null;
            //     try {
            //         File dumpFile = File.createTempFile ("bad", ".gz", new File ("data"));
            //         fileOutputStream = new FileOutputStream (dumpFile);
            //         fileOutputStream.write (bytes);
            //     } finally {
            //         closeObject (fileOutputStream);
            //         throw exception; // thrown further, no output required here
            //     }
        } finally {
            closeObject(byteArrayInputStream);

            if (!closeObject(bufferedInputStream)) {
                //noinspection ConstantConditions
                closeObjects(gZIPInputStream, inflaterInputStream);
            }
            closeObject(byteArrayOutputStream);
        }

        return returnValue;
    }

    @NotNull
    public static LinkedList<String> getSubstrings(final String inputString, final String firstDelimiter, final String secondDelimiter) {
        return getSubstrings(inputString, inputString, firstDelimiter, secondDelimiter, false, -1);
    }

    @NotNull
    public static LinkedList<String> getSubstrings(final String inputString, final String firstDelimiter, final String secondDelimiter, final boolean getInterSubstrings) {
        return getSubstrings(inputString, inputString, firstDelimiter, secondDelimiter, getInterSubstrings, -1);
    }

    @NotNull
    public static LinkedList<String> getSubstrings(final String harvestInputString, final String searchInputString, final String firstDelimiter, final String secondDelimiter, final boolean getInterSubstrings, final int nSubstrings) {
        // harvestInputString is the string the substrings will be harvested from
        // searchInputString is the string the searches will be made on
        //     - this would usually be the same as harvestInputString, but there can be exceptions, for example when we want the letterCase to differ
        // excluding the delimiters
        // if getInterSubstrings flag is set, odd positions, including first and last position, are interSubstrings, while even positions are substrings
        // maximum nSubstrings are taken
        //     - interSubstrings are counted as well
        //     - negative nSubstrings takes maximum possible
        final LinkedList<String> returnSet = new LinkedList<>();

        if (harvestInputString != null && searchInputString != null) {
            int beginIndex = 0, firstDelimiterIndex, secondDelimiterIndex, substringsCounter = 0;

            firstDelimiterIndex = searchInputString.indexOf(firstDelimiter, beginIndex);
            secondDelimiterIndex = firstDelimiterIndex >= 0 ? searchInputString.indexOf(secondDelimiter, firstDelimiterIndex + firstDelimiter.length()) : -1;

            while ((nSubstrings < 0 || substringsCounter < nSubstrings) && firstDelimiterIndex >= 0 && secondDelimiterIndex >= 0) {
                if (getInterSubstrings) {
                    substringsCounter++;
                    returnSet.add(new String(harvestInputString.substring(beginIndex, firstDelimiterIndex)));
                }
                if (nSubstrings < 0 || substringsCounter < nSubstrings) {
                    substringsCounter++;
                    returnSet.add(new String(harvestInputString.substring(firstDelimiterIndex + firstDelimiter.length(), secondDelimiterIndex)));

                    beginIndex = searchInputString.indexOf(secondDelimiter, firstDelimiterIndex + firstDelimiter.length()) + secondDelimiter.length();

                    firstDelimiterIndex = searchInputString.indexOf(firstDelimiter, beginIndex);
                    secondDelimiterIndex = firstDelimiterIndex >= 0 ? searchInputString.indexOf(secondDelimiter, firstDelimiterIndex + firstDelimiter.length()) : -1;
                }
            } // end while

            if (getInterSubstrings && (nSubstrings < 0 || substringsCounter < nSubstrings)) {
                returnSet.add(new String(harvestInputString.substring(beginIndex)));
            }
        }

        return returnSet;
    }

    @NotNull
    public static LinkedList<String> getSubstringsIgnoreCase(final String inputString, @NotNull final String firstDelimiter, @NotNull final String secondDelimiter) {
        return getSubstrings(inputString, inputString.toLowerCase(Locale.ENGLISH), firstDelimiter.toLowerCase(Locale.ENGLISH), secondDelimiter.toLowerCase(Locale.ENGLISH), false, -1);
    }

    public static String getSubstring(final String inputString, final String firstDelimiter, final String secondDelimiter) {
        String returnString = null;

        try {
            returnString = getSubstrings(inputString, inputString, firstDelimiter, secondDelimiter, false, 1).iterator().next();
        } catch (java.util.NoSuchElementException noSuchElementException) {
            logger.error("STRANGE ERROR inside getSubstring: {}", new Object[]{inputString, firstDelimiter, secondDelimiter}, noSuchElementException);
        }

        return returnString;
    }

    public static String removeSubstring(final String inputString, final String firstDelimiter, final String secondDelimiter) {
        return removeSubstring(inputString, firstDelimiter, secondDelimiter, "");
    }

    public static String removeSubstring(final String inputString, final String firstDelimiter, final String secondDelimiter, final String replacement) { // in current form, it just removes the first occurrence, and leaves delimiters in place
        @Nullable final String modifiedString;
        if (inputString == null || firstDelimiter == null || secondDelimiter == null || replacement == null) {
            logger.error("null argument in removeSubstring for: {} {} {} {}", inputString, firstDelimiter, secondDelimiter, replacement);
            modifiedString = null;
        } else {
            final int firstDelimiterLength = firstDelimiter.length();
            final int firstIndex = inputString.indexOf(firstDelimiter);
            final int secondIndex = inputString.indexOf(secondDelimiter, firstIndex + firstDelimiterLength);
            if (firstIndex >= 0 && secondIndex > 0 && secondIndex > firstIndex) {
                modifiedString = inputString.substring(0, firstIndex + firstDelimiterLength) + replacement + inputString.substring(secondIndex);
            } else {
                modifiedString = inputString;
                logger.error("bad indexes in removeSubstring: {} {} {} {} {} {}", firstIndex, secondIndex, inputString, firstDelimiter, secondDelimiter, replacement);
            }
        }

        return modifiedString;
    }

    @SuppressWarnings({"resource", "IOResourceOpenedButNotSafelyClosed"})
    public static <T extends Serializable> T serializedDeepCopy(final T sourceObject) {
        // sourceObject must be serializable
        ObjectOutputStream objectOutputStream = null;
        ObjectInputStream objectInputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        T returnValue = null;

        if (sourceObject != null) {
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

                // serialize and write sourceObject to byteArrayOutputStream
                objectOutputStream.writeObject(sourceObject);

                // always flush your stream
                objectOutputStream.flush();
                byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                objectInputStream = new ObjectInputStream(byteArrayInputStream);

                // read the serialized, and deep copied, object
                @SuppressWarnings("unchecked") final T temporaryReturnValue = (T) objectInputStream.readObject(); // the temporary var is created only to attach the SuppressWarnings annotation
                returnValue = temporaryReturnValue;
            } catch (IOException | ClassNotFoundException exception) {
                logger.error("STRANGE ERROR inside serializedDeepCopy: {}", new Object[]{sourceObject.getClass(), sourceObject}, exception);
            } finally {
                // always close your streams in finally clauses
                if (!closeObject(objectOutputStream)) {
                    closeObject(byteArrayOutputStream);
                }
                if (!closeObject(objectInputStream)) {
                    closeObject(byteArrayInputStream);
                }
            }
        } else {
            logger.error("STRANGE sourceObject null in serializedDeepCopy, timeStamp={}", System.currentTimeMillis());
        }

        return returnValue;
    }

    @SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "NestedTryStatement"})
    public static <T> void synchronizedCopyObjectFields(final T sourceObject, final T destinationObject) {
        try {
            if (sourceObject != null && destinationObject != null) {
                try {
                    final Class<?> sourceClass = sourceObject.getClass();
                    final Object tempObject = sourceClass.getDeclaredConstructor().newInstance();

                    synchronized (sourceObject) {
                        copyObjectFields(sourceObject, tempObject);
                    }
                    synchronized (destinationObject) {
                        copyObjectFields(tempObject, destinationObject);
                    }
                } catch (InstantiationException instantiationException) {
                    logger.error("InstantiationException in synchronizedCopyObjectFields ({} might not be public)", sourceObject.getClass(), instantiationException);
                    copyObjectFields(sourceObject, destinationObject);
                } catch (NoSuchMethodException e) {
                    logger.error("NoSuchMethodException in synchronizedCopyObjectFields {}", sourceObject.getClass(), e);
                    copyObjectFields(sourceObject, destinationObject);
                } catch (InvocationTargetException e) {
                    logger.error("InvocationTargetException in synchronizedCopyObjectFields {}", sourceObject.getClass(), e);
                    copyObjectFields(sourceObject, destinationObject);
                }
            } else {
                logger.error("STRANGE sourceObject or destinationObject null in synchronizedCopyObjectFields, {} {} timeStamp={}", sourceObject, destinationObject, System.currentTimeMillis());
            }
        } catch (IllegalAccessException illegalAccessException) {
            logger.error("IllegalAccessException in synchronizedCopyObjectFields", illegalAccessException);
        }
    }

    @SuppressWarnings("OverlyNestedMethod")
    public static <T> void copyObjectFields(final T sourceObject, final T destinationObject) {
        // not synchronized
        // fields from sourceObject are copied one by one to destinationObject
        // if either of the objects is null, no action is taken
        // shallow copy
        // doesn't work for static or final fields
        try {
            if (sourceObject != null && destinationObject != null) {
                final Class<?> sourceClass = sourceObject.getClass();
                final Class<?> destinationClass = destinationObject.getClass();
                final Field[] sourceFieldsArray = sourceClass.getDeclaredFields();
                final Field[] destinationFieldsArray = destinationClass.getDeclaredFields();
                @SuppressWarnings("NumericCastThatLosesPrecision") final Collection<String> destinationFieldNamesSet = new LinkedHashSet<>((int) ceilingPowerOf(2, destinationFieldsArray.length / 0.75), 0.75f);

                for (final Field destinationField : destinationFieldsArray) {
                    destinationFieldNamesSet.add(destinationField.getName());
                }
                for (final Field sourceField : sourceFieldsArray) {
                    sourceField.setAccessible(true);
                    final String sourceFieldName = sourceField.getName();
                    if (destinationFieldNamesSet.contains(sourceFieldName)) {
                        final Field destinationField = destinationClass.getDeclaredField(sourceFieldName);
                        destinationField.setAccessible(true);
                        final int destinationModifiers = destinationField.getModifiers();
                        final Class<?> fieldClass = sourceField.getType();

                        if (fieldClass.equals(destinationField.getType()) && !Modifier.isFinal(destinationModifiers) && !Modifier.isStatic(destinationModifiers) &&
                            !Modifier.isTransient(destinationModifiers)) {
                            if (fieldClass.equals(boolean.class)) {
                                destinationField.setBoolean(destinationObject, sourceField.getBoolean(sourceObject));
                            } else if (fieldClass.equals(byte.class)) {
                                destinationField.setByte(destinationObject, sourceField.getByte(sourceObject));
                            } else if (fieldClass.equals(char.class)) {
                                destinationField.setChar(destinationObject, sourceField.getChar(sourceObject));
                            } else if (fieldClass.equals(double.class)) {
                                destinationField.setDouble(destinationObject, sourceField.getDouble(sourceObject));
                            } else if (fieldClass.equals(float.class)) {
                                destinationField.setFloat(destinationObject, sourceField.getFloat(sourceObject));
                            } else if (fieldClass.equals(int.class)) {
                                destinationField.setInt(destinationObject, sourceField.getInt(sourceObject));
                            } else if (fieldClass.equals(long.class)) {
                                destinationField.setLong(destinationObject, sourceField.getLong(sourceObject));
                            } else if (fieldClass.equals(short.class)) {
                                destinationField.setShort(destinationObject, sourceField.getShort(sourceObject));
                            } else {
                                destinationField.set(destinationObject, sourceField.get(sourceObject));
                            }
                        }
                    } // end if set.contains
                } // end for
            } else {
                logger.error("STRANGE sourceObject or destinationObject null in copyObjectFields, {} {} timeStamp={}", sourceObject, destinationObject, System.currentTimeMillis());
            }
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException exception) {
            logger.error("STRANGE ERROR inside copyObjectFields: {}", new Object[]{sourceObject != null ? sourceObject.getClass() : null, sourceObject, destinationObject != null ? destinationObject.getClass() : null, destinationObject}, exception);
        }
    }

    public static String objectToString(final Object object, final String... excludePatterns) {
        // default to printing default value fields and final fields, but not use toString method
        return objectToString(object, true, true, true, 0, excludePatterns);
    }

    public static String objectToString(final Object object) {
        // default to printing default value fields and final fields, but not use toString method
        return objectToString(object, true, true, true, 0);
    }

    public static String objectToString(final Object object, final boolean printDefaultValueFields) {
        return objectToString(object, printDefaultValueFields, true, true, 0);
    }

    public static String objectToString(final Object object, final boolean printDefaultValueFields, final boolean printFinalFields) {
        return objectToString(object, printDefaultValueFields, printFinalFields, true, 0);
    }

    @Nullable
    @SuppressWarnings({"ConstantConditions", "OverlyComplexMethod", "OverlyLongMethod", "OverlyNestedMethod", "NestedTryStatement"})
    public static String objectToString(final Object object, final boolean printDefaultValueFields, final boolean printFinalFields, final boolean useToStringMethod, final int recursionCounter, final String... excludePatterns) {
        // synchronized on object
        // has option to use the default toString() method, disabled by default
        // does check to print final type fields
        // does check to print fields with the default value (null, 0, false)
        // has fail-safe against infinite recursion, max 10 levels
        // out of memory protection, max 4Mb
        @Nullable StringBuilder returnStringBuilder;

        if (recursionCounter > 10) {
            returnStringBuilder = new StringBuilder("recursionCounter=" + recursionCounter + " is too high!");
        } else {
            try {
                if (object != null) {
                    final Class<?> objectClass = object.getClass();
                    @Nullable Method toStringMethod;

                    if (useToStringMethod) {
                        try {
                            toStringMethod = objectClass.getDeclaredMethod("toString");
                            // some default toString methods have undesired print, better leave them unused
//                            if (!toStringMethod.isAccessible()) {
//                                toStringMethod.setAccessible(true);
//                            }
                        } catch (NoSuchMethodException noSuchMethodException) {
                            toStringMethod = null;
                        } catch (SecurityException securityException) {
                            logger.error("STRANGE securityException inside objectToString getDeclaredMethod: {} {}", objectClass, object, securityException);
                            toStringMethod = null;
                        } // catch (Throwable throwable) {
                        //     logger.error("STRANGE ERROR inside objectToString getDeclaredMethod: {} {}", objectClass, object, throwable);
                        //     toStringMethod = null;
                        // }
                    } else {
                        toStringMethod = null;
                    }

                    if (toStringMethod == null || !toStringMethod.canAccess(object)) {
                        returnStringBuilder = new StringBuilder(32);

                        if (object instanceof TreeItem<?>) {
                            returnStringBuilder.append(object); // calls toString method, else the TreeItem object is too big and crashes the thread
                        } else if (object instanceof Collection<?>) {
                            final Object[] arrayValue = ((Collection<?>) object).toArray(); // never null due to instanceof always being false for null
                            returnStringBuilder.append(objectToString(arrayValue, printDefaultValueFields, printFinalFields, useToStringMethod, recursionCounter + 1, excludePatterns));
                        } else if (object instanceof Map<?, ?>) {
                            final Object[] arrayValue = ((Map<?, ?>) object).entrySet().toArray(); // never null due to instanceof always being false for null
                            returnStringBuilder.append(objectToString(arrayValue, printDefaultValueFields, printFinalFields, useToStringMethod, recursionCounter + 1, excludePatterns));
                        } else if (object instanceof Entry<?, ?>) {
                            final Entry<?, ?> entry = (Entry<?, ?>) object;
                            final Object key = entry.getKey(); // never null due to instanceof always being false for null
                            final Object value = entry.getValue(); // never null due to instanceof always being false for null
                            returnStringBuilder.append("(key=").
                                    append(objectToString(key, printDefaultValueFields, printFinalFields, useToStringMethod, recursionCounter + 1, excludePatterns)).
                                                       append(" value=").append(objectToString(value, printDefaultValueFields, printFinalFields, useToStringMethod, recursionCounter + 1, excludePatterns)).append(") ");
                        } else if (objectClass.equals(Date.class)) {
                            final String stringValue = object.toString();
                            returnStringBuilder.append(stringValue).append(" ");
                        } else if (objectClass.equals(String.class)) {
                            final String stringValue = object.toString();
                            returnStringBuilder.append(stringValue).append(" ");
                        } else if (objectClass.equals(boolean.class) || objectClass.equals(byte.class) || objectClass.equals(char.class) || objectClass.equals(double.class) ||
                                   objectClass.equals(float.class) || objectClass.equals(int.class) || objectClass.equals(long.class) || objectClass.equals(short.class)) {
                            returnStringBuilder.append(object).append(" ");
                        } else if (objectClass.equals(Boolean.class)) {
                            final Boolean booleanValue = (Boolean) object;
                            returnStringBuilder.append(booleanValue.booleanValue()).append(" ");
                        } else if (objectClass.equals(Byte.class)) {
                            final Byte byteValue = (Byte) object;
                            returnStringBuilder.append(byteValue.byteValue()).append(" ");
                        } else if (objectClass.equals(Double.class)) {
                            final Double doubleValue = (Double) object;
                            returnStringBuilder.append(doubleValue.doubleValue()).append(" ");
                        } else if (objectClass.equals(Float.class)) {
                            final Float floatValue = (Float) object;
                            returnStringBuilder.append(floatValue.floatValue()).append(" ");
                        } else if (objectClass.equals(Integer.class)) {
                            final Integer integerValue = (Integer) object;
                            returnStringBuilder.append(integerValue.intValue()).append(" ");
                        } else if (objectClass.equals(Long.class)) {
                            final Long longValue = (Long) object;
                            returnStringBuilder.append(longValue.longValue()).append(" ");
                        } else if (objectClass.equals(Short.class)) {
                            final Short shortValue = (Short) object;
                            returnStringBuilder.append(shortValue.shortValue()).append(" ");
                        } else {
                            if (objectClass.equals(char[].class)) {
                                returnStringBuilder.append((char[]) object).append(" ");
                            } else if (objectClass.equals(byte[].class)) {
                                returnStringBuilder.append(new String((byte[]) object, StandardCharsets.US_ASCII)).append(" ");
                            } else if (objectClass.equals(boolean[].class)) {
                                returnStringBuilder.append(Arrays.toString((boolean[]) object)).append(" ");
                            } else if (objectClass.equals(double[].class)) {
                                returnStringBuilder.append(Arrays.toString((double[]) object)).append(" ");
                            } else if (objectClass.equals(float[].class)) {
                                returnStringBuilder.append(Arrays.toString((float[]) object)).append(" ");
                            } else if (objectClass.equals(int[].class)) {
                                returnStringBuilder.append(Arrays.toString((int[]) object)).append(" ");
                            } else if (objectClass.equals(long[].class)) {
                                returnStringBuilder.append(Arrays.toString((long[]) object)).append(" ");
                            } else if (objectClass.equals(short[].class)) {
                                returnStringBuilder.append(Arrays.toString((short[]) object)).append(" ");
                            } else if (objectClass.isArray()) {
                                returnStringBuilder.append("[");

                                final Object[] objectArray = (Object[]) object;
                                int appendedCounter = 0;
                                for (final Object o : objectArray) {
                                    if (o != null) {
                                        if (appendedCounter > 0) {
                                            returnStringBuilder.append(", ");
                                        }
                                        returnStringBuilder.append(objectToString(o, printDefaultValueFields, printFinalFields, useToStringMethod, recursionCounter + 1, excludePatterns));
                                        appendedCounter++;
                                    }
                                }
                                returnStringBuilder.append("] ");
                            } else if (object instanceof Enum<?>) {
                                final String name = ((Enum<?>) object).name(); // never null due to instanceof always being false for null
                                returnStringBuilder.append(name).append(" ");
                            } else if (object instanceof Class<?>) {
                                final String name = ((Class<?>) object).getName(); // never null due to instanceof always being false for null
                                returnStringBuilder.append(name).append(" ");
                            } else { // regular object
                                returnStringBuilder.append("(");

                                final List<Class<?>> classList = new ArrayList<>(2);
                                Class<?> localClass = objectClass;
                                do {
                                    classList.add(localClass);
                                    localClass = localClass.getSuperclass();
                                } while (localClass != null && localClass != Object.class);

                                for (int counter = classList.size() - 1; counter >= 0; counter--) {
                                    final Class<?> clazz = classList.get(counter);
                                    final Field[] fieldsArray = clazz.getDeclaredFields();

                                    // synchronized (object) {
                                    // synchronization removed as it's deadlock prone; it can be added externally
                                    for (final Field field : fieldsArray) {
                                        field.trySetAccessible();

                                        final Class<?> fieldClass = field.getType();
                                        final String fieldName = field.getName();
                                        final int fieldModifiers = field.getModifiers();

                                        boolean excludeField = false;
                                        if (excludePatterns != null && excludePatterns.length > 0) {
                                            for (final String excludePattern : excludePatterns) {
                                                if (fieldName.contains(excludePattern)) {
                                                    excludeField = true;
                                                    break;
                                                }
                                            } // end for
                                        } else { // no excludePatterns
                                        }

                                        if (excludeField || field.isSynthetic() ||
                                            ((Modifier.isTransient(fieldModifiers) || Modifier.isStatic(fieldModifiers) || fieldName.contains("$") || "logger".equals(fieldName)) &&
                                             Modifier.isFinal(fieldModifiers))) {
                                            // no need to print these fields
                                        } else if (printFinalFields || !Modifier.isFinal(fieldModifiers)) {
                                            if (Collection.class.isAssignableFrom(fieldClass)) {
                                                final Object objectValue = field.get(object);
                                                final Object[] arrayValue = objectValue == null ? null : ((Collection<?>) objectValue).toArray();
                                                returnStringBuilder.append(fieldName).append("=").
                                                        append(objectToString(arrayValue, printDefaultValueFields, printFinalFields, useToStringMethod, recursionCounter + 1, excludePatterns)).append(" ");
                                            } else if (Map.class.isAssignableFrom(fieldClass)) {
                                                final Object objectValue = field.get(object);
                                                final Object[] arrayValue = objectValue == null ? null : ((Map<?, ?>) objectValue).entrySet().toArray();
                                                returnStringBuilder.append(fieldName).append("=").
                                                        append(objectToString(arrayValue, printDefaultValueFields, printFinalFields, useToStringMethod, recursionCounter + 1, excludePatterns)).append(" ");
                                            } else if (Entry.class.isAssignableFrom(fieldClass)) {
                                                final Entry<?, ?> entry = (Entry<?, ?>) field.get(object);
                                                final Object key = entry == null ? null : entry.getKey();
                                                final Object value = entry == null ? null : entry.getValue();
                                                returnStringBuilder.append(fieldName).append("=(key=").
                                                        append(objectToString(key, printDefaultValueFields, printFinalFields, useToStringMethod, recursionCounter + 1, excludePatterns)).append(" value=").
                                                                           append(objectToString(value, printDefaultValueFields, printFinalFields, useToStringMethod, recursionCounter + 1, excludePatterns)).append(") ");
                                            } else if (fieldClass.equals(String.class)) {
                                                final Object objectValue = field.get(object);
                                                final String stringValue = objectValue == null ? null : objectValue.toString();
                                                if (printDefaultValueFields || stringValue != null) {
                                                    returnStringBuilder.append(fieldName).append("=").append(stringValue).append(" ");
                                                } else { // !printDefaultValueFields && stringValue == null , this won't be printed
                                                }
                                            } else if (fieldClass.equals(Date.class)) {
                                                final Object objectValue = field.get(object);
                                                final String stringValue = objectValue == null ? null : objectValue.toString();
                                                if (printDefaultValueFields || stringValue != null) {
                                                    returnStringBuilder.append(fieldName).append("=").append(stringValue).append(" ");
                                                } else { // !printDefaultValueFields && stringValue == null , this won't be printed
                                                }
                                            } else if (fieldClass.equals(boolean.class)) {
                                                final boolean booleanValue = field.getBoolean(object);
                                                if (printDefaultValueFields || booleanValue) {
                                                    returnStringBuilder.append(fieldName).append("=").append(booleanValue).append(" ");
                                                } else { // !printDefaultValueFields && !booleanValue , this won't be printed
                                                }
                                            } else if (fieldClass.equals(byte.class)) {
                                                final byte byteValue = field.getByte(object);
                                                if (printDefaultValueFields || byteValue != 0) {
                                                    returnStringBuilder.append(fieldName).append("=").append(byteValue).append(" ");
                                                } else { // !printDefaultValueFields && byteValue == 0 , this won't be printed
                                                }
                                            } else if (fieldClass.equals(char.class)) {
                                                final char charValue = field.getChar(object);
                                                if (printDefaultValueFields || charValue != 0) {
                                                    returnStringBuilder.append(fieldName).append("=").append(charValue).append(" ");
                                                } else { // !printDefaultValueFields && charValue == 0 , this won't be printed
                                                }
                                            } else if (fieldClass.equals(double.class)) {
                                                final double doubleValue = field.getDouble(object);
                                                if (printDefaultValueFields || doubleValue != 0) {
                                                    returnStringBuilder.append(fieldName).append("=").append(doubleValue).append(" ");
                                                } else { // !printDefaultValueFields && doubleValue == 0 , this won't be printed
                                                }
                                            } else if (fieldClass.equals(float.class)) {
                                                final float floatValue = field.getFloat(object);
                                                if (printDefaultValueFields || floatValue != 0) {
                                                    returnStringBuilder.append(fieldName).append("=").append(floatValue).append(" ");
                                                } else { // !printDefaultValueFields && floatValue == 0 , this won't be printed
                                                }
                                            } else if (fieldClass.equals(int.class)) {
                                                final int intValue = field.getInt(object);
                                                if (printDefaultValueFields || intValue != 0) {
                                                    returnStringBuilder.append(fieldName).append("=").append(intValue).append(" ");
                                                } else { // !printDefaultValueFields && intValue == 0 , this won't be printed
                                                }
                                            } else if (fieldClass.equals(long.class)) {
                                                final long longValue = field.getLong(object);
                                                if (printDefaultValueFields || longValue != 0) {
                                                    returnStringBuilder.append(fieldName).append("=").append(longValue).append(" ");
                                                } else { // !printDefaultValueFields && longValue == 0 , this won't be printed
                                                }
                                            } else if (fieldClass.equals(short.class)) {
                                                final short shortValue = field.getShort(object);
                                                if (printDefaultValueFields || shortValue != 0) {
                                                    returnStringBuilder.append(fieldName).append("=").append(shortValue).append(" ");
                                                } else { // !printDefaultValueFields && shortValue == 0 , this won't be printed
                                                }
                                            } else if (fieldClass.equals(Boolean.class)) {
                                                final Boolean booleanValue = (Boolean) field.get(object);
                                                if (printDefaultValueFields || booleanValue != null) {
                                                    returnStringBuilder.append(fieldName).append("=").append(booleanValue).append(" ");
                                                } else { // !printDefaultValueFields && booleanValue == null , this won't be printed
                                                }
                                            } else if (fieldClass.equals(Byte.class)) {
                                                final Byte byteValue = (Byte) field.get(object);
                                                if (printDefaultValueFields || byteValue != null) {
                                                    returnStringBuilder.append(fieldName).append("=").append(byteValue).append(" ");
                                                } else { // !printDefaultValueFields && byteValue == null , this won't be printed
                                                }
                                            } else if (fieldClass.equals(Double.class)) {
                                                final Double doubleValue = (Double) field.get(object);
                                                if (printDefaultValueFields || doubleValue != null) {
                                                    returnStringBuilder.append(fieldName).append("=").append(doubleValue).append(" ");
                                                } else { // !printDefaultValueFields && doubleValue == null , this won't be printed
                                                }
                                            } else if (fieldClass.equals(Float.class)) {
                                                final Float floatValue = (Float) field.get(object);
                                                if (printDefaultValueFields || floatValue != null) {
                                                    returnStringBuilder.append(fieldName).append("=").append(floatValue).append(" ");
                                                } else { // !printDefaultValueFields && floatValue == null , this won't be printed
                                                }
                                            } else if (fieldClass.equals(Integer.class)) {
                                                final Integer integerValue = (Integer) field.get(object);
                                                if (printDefaultValueFields || integerValue != null) {
                                                    returnStringBuilder.append(fieldName).append("=").append(integerValue).append(" ");
                                                } else { // !printDefaultValueFields && integerValue == null , this won't be printed
                                                }
                                            } else if (fieldClass.equals(Long.class)) {
                                                final Long longValue = (Long) field.get(object);
                                                if (printDefaultValueFields || longValue != null) {
                                                    returnStringBuilder.append(fieldName).append("=").append(longValue).append(" ");
                                                } else { // !printDefaultValueFields && longValue == null , this won't be printed
                                                }
                                            } else if (fieldClass.equals(Short.class)) {
                                                final Short shortValue = (Short) field.get(object);
                                                if (printDefaultValueFields || shortValue != null) {
                                                    returnStringBuilder.append(fieldName).append("=").append(shortValue).append(" ");
                                                } else { // !printDefaultValueFields && shortValue == null , this won't be printed
                                                }
                                            } else {
                                                Object objectValue = field.get(object);
                                                if (objectValue == object) {
                                                    objectValue = "SAME OBJECT"; // avoid infinite recursion
                                                }
                                                if (printDefaultValueFields || objectValue != null) {
                                                    if (fieldClass.equals(char[].class)) {
                                                        returnStringBuilder.append(fieldName).append("=").append((char[]) objectValue).append(" ");
                                                    } else if (fieldClass.equals(byte[].class)) {
                                                        returnStringBuilder.append(fieldName).append("=").append(objectValue == null ? null : new String((byte[]) objectValue, StandardCharsets.US_ASCII)).append(" ");
                                                    } else if (fieldClass.equals(boolean[].class)) {
                                                        returnStringBuilder.append(fieldName).append("=").append(Arrays.toString((boolean[]) objectValue)).append(" ");
                                                    } else if (fieldClass.equals(double[].class)) {
                                                        returnStringBuilder.append(fieldName).append("=").append(Arrays.toString((double[]) objectValue)).append(" ");
                                                    } else if (fieldClass.equals(float[].class)) {
                                                        returnStringBuilder.append(fieldName).append("=").append(Arrays.toString((float[]) objectValue)).append(" ");
                                                    } else if (fieldClass.equals(int[].class)) {
                                                        returnStringBuilder.append(fieldName).append("=").append(Arrays.toString((int[]) objectValue)).append(" ");
                                                    } else if (fieldClass.equals(long[].class)) {
                                                        returnStringBuilder.append(fieldName).append("=").append(Arrays.toString((long[]) objectValue)).append(" ");
                                                    } else if (fieldClass.equals(short[].class)) {
                                                        returnStringBuilder.append(fieldName).append("=").append(Arrays.toString((short[]) objectValue)).append(" ");
                                                    } else if (fieldClass.isArray()) {
                                                        returnStringBuilder.append(fieldName).append("[");

                                                        final Object[] objectArray = (Object[]) objectValue;
                                                        final int objectArrayLength = objectArray == null ? -1 : objectArray.length;
                                                        int appendedCounter = 0;
                                                        for (int i = 0; i < objectArrayLength; i++) {
                                                            if (objectArray[i] != null) {
                                                                if (appendedCounter > 0) {
                                                                    returnStringBuilder.append(", ");
                                                                }
                                                                returnStringBuilder.append(objectToString(objectArray[i], printDefaultValueFields, printFinalFields, useToStringMethod, recursionCounter + 1, excludePatterns));
                                                                appendedCounter++;
                                                            }
                                                        }
                                                        returnStringBuilder.append("] ");
                                                    } else {
                                                        returnStringBuilder.append(fieldName).append("=").
                                                                append(objectToString(objectValue, printDefaultValueFields, printFinalFields, useToStringMethod, recursionCounter + 1, excludePatterns)).append(" ");
                                                    }
                                                } else { // !printDefaultValueFields && objectValue == null , this won't be printed
                                                }
                                            } // end else
                                        } else { // !printFinalFields && Modifier.isFinal(fieldModifiers , this won't be printed
                                        }
                                    } // end for
                                } // end for
                                // } // end synchronized block    
                                final int length = returnStringBuilder.length();
                                if (length > 0 && returnStringBuilder.charAt(length - 1) == ' ') {
                                    returnStringBuilder.setLength(length - 1);
                                }
                                returnStringBuilder.append(")");
                            } // end else regular object
                        } // end else

                        // returnStringBuilder = new StringBuilder(returnStringBuilder.toString().trim());
                        // I trim in the end, as I return the value
                    } else {
                        // this is supposedly already synchronized on object in the classes I create
                        returnStringBuilder = new StringBuilder((String) toStringMethod.invoke(object));
                    }
                } else { // object == null
                    returnStringBuilder = null;
                }
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException exception) {
                logger.error("STRANGE ERROR inside objectToString: {}", new Object[]{object.getClass(), object}, exception);
                returnStringBuilder = null;
            }
        }
        if (returnStringBuilder != null) {
            // out of memory protection
            final int returnStringBuilderLength = returnStringBuilder.length();
            if (returnStringBuilderLength > 4194304) {
                // logger.error("objectTooLarge: {}", returnStringBuilder.toString()); // printing this would fill the HDD fairly fast in case of error
                returnStringBuilder = new StringBuilder("returnStringBuilderLength=" + returnStringBuilderLength + " would lead to out of memory error!");
            }
        }

        return returnStringBuilder == null ? null : returnStringBuilder.toString().trim();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T[] arrayMerge(final T[]... arrays) {
        @Nullable final T[] mergedArray;
        if (arrays == null) {
            logger.error("null arrays in arrayMerge");
            mergedArray = null;
        } else {
            // Determine required size of new array
            int count = 0;
            Class<T> clazz = null;
            for (final T[] array : arrays) {
                if (array == null) { // might be normal, nothing to do
                } else {
                    final int length = array.length;
                    count += length;
                    for (int i = 0; clazz == null && i < length; i++) {
                        if (array[i] == null) { // nothing to do with this element
                        } else {
                            clazz = (Class<T>) array[i].getClass();
                        }
                    }
                }
            }

            // create new array of required class
            if (clazz == null) {
                logger.error("null clazz in arrayMerge for: {}", objectToString(arrays));
                mergedArray = null;
            } else {
                mergedArray = (T[]) Array.newInstance(clazz, count);

                // Merge each array into new array
                int start = 0;
                for (final T[] array : arrays) {
                    if (array == null) { // useless array
                    } else {
                        final int length = array.length;
                        System.arraycopy(array, 0, mergedArray, start, length);
                        start += length;
                    }
                }
            }
        }
        return mergedArray;
    }

    @Nullable
    public static TrustManager[] getCustomTrustManager(final String keyStoreFileName, final String keyStorePassword) {
        @Nullable final TrustManager[] result;
        if (keyStoreFileName == null) {
            logger.error("null fileName in getCustomTrustManager");
            result = null;
        } else {
            X509TrustManager customTm = null;
            X509TrustManager myTm = null;
            try {
                final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                // Using null here initialises the TMF with the default trust store.
                tmf.init((KeyStore) null);

                // Get hold of the default trust manager
                X509TrustManager defaultTm = null;
                for (final TrustManager tm : tmf.getTrustManagers()) {
                    if (tm instanceof X509TrustManager) {
                        defaultTm = (X509TrustManager) tm;
                        break;
                    }
                }

                final FileInputStream myKeys = new FileInputStream(keyStoreFileName);
                final KeyStore myTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                myTrustStore.load(myKeys, keyStorePassword == null ? null : keyStorePassword.toCharArray());
                myKeys.close();
                final TrustManagerFactory secondTmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                secondTmf.init(myTrustStore);

                for (final TrustManager tm : secondTmf.getTrustManagers()) {
                    if (tm instanceof X509TrustManager) {
                        myTm = (X509TrustManager) tm;
                        break;
                    }
                }

                // Wrap it in your own class; I need final variables for that
                final X509TrustManager finalDefaultTm = defaultTm;
                final X509TrustManager finalMyTm = myTm;
                //noinspection OverlyComplexAnonymousInnerClass
                customTm = new X509TrustManager() {
                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return arrayMerge(finalMyTm.getAcceptedIssuers(), finalDefaultTm.getAcceptedIssuers());
                    }

                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public void checkServerTrusted(final X509Certificate[] x509Certificates, @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") final String authType)
                            throws CertificateException {
                        try {
                            finalMyTm.checkServerTrusted(x509Certificates, authType);
                        } catch (CertificateException e) {
                            // This will throw another CertificateException if this fails too.
                            finalDefaultTm.checkServerTrusted(x509Certificates, authType);
                        }
                    }

                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public void checkClientTrusted(final X509Certificate[] x509Certificates, @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") final String authType)
                            throws CertificateException {
                        try {
                            finalMyTm.checkClientTrusted(x509Certificates, authType);
                        } catch (CertificateException e) {
                            // This will throw another CertificateException if this fails too.
                            finalDefaultTm.checkClientTrusted(x509Certificates, authType);
                        }
                    }
                }; // end of anonymous inner class
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception e) {
                logger.error("exception in getCustomTrustManager for: {} {}", keyStoreFileName, keyStorePassword, e);
            }
            result = customTm == null ? null : new TrustManager[]{customTm};
        }

        return result;
    }

    @NotNull
    public static TrustManager[] getTrustAllCertsManager() {
        // Create a trust manager that does not validate certificate chains
        return new TrustManager[]{
                new X509TrustManager() {
                    @Nullable
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(final java.security.cert.X509Certificate[] x509Certificates, @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") final String authType) {
                    }

                    @Override
                    public void checkServerTrusted(final java.security.cert.X509Certificate[] x509Certificates, @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter") final String authType) {
                    }
                }
        };
    }

    public static void disableHTTPSValidation() {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCertsManager = getTrustAllCertsManager();

        // Install the all-trusting trust manager
        try {
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCertsManager, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException exception) {
            logger.error("STRANGE ERROR inside disableHTTPSValidation()", exception);
        }

        // should avoid the "HTTPS hostname wrong" exception
        HttpsURLConnection.setDefaultHostnameVerifier((verifyString, sslSession) -> true);
    }

    @SuppressWarnings("NestedTryStatement")
    public static String specialCharParser(final String line) {
        String result = line;

        if (result != null) {
            try {
                final Map<String, String> breakPoints = new LinkedHashMap<>(8, 0.75f);
                breakPoints.put("&nbsp;", " ");
                breakPoints.put("&amp;", "&");
                breakPoints.put("&quot;", "\"");
                breakPoints.put("&lt;", "<");
                breakPoints.put("&gt;", ">");

                int modified = 1;
                while (modified > 0) {
                    modified = 0;

                    while (result.contains("&#") && result.indexOf(';', result.indexOf("&#") + "&#".length()) >= 0) {
                        final String tempString = result.substring(result.indexOf("&#") + "&#".length(), result.indexOf(';', result.indexOf("&#") + "&#".length()));
                        final int tempInt;

                        try {
                            tempInt = tempString.indexOf('x') == 0 ? Integer.parseInt(tempString.substring(tempString.indexOf('x') + "x".length()), 16) : Integer.parseInt(tempString);

                            //noinspection NumericCastThatLosesPrecision
                            result = result.substring(0, result.indexOf("&#")) + (char) tempInt +
                                     result.substring(result.indexOf(';', result.indexOf("&#") + "&#".length()) + ";".length());
                            modified++;
                        } catch (NumberFormatException numberFormatException) {
                            result = result.substring(0, result.indexOf("&#")) + " " + result.substring(result.indexOf("&#") + "&#".length());
                            modified++;
                        }
                    }

                    for (final Entry<String, String> entry : breakPoints.entrySet()) {
                        final String element = entry.getKey();
                        if (result.contains(element)) {
                            result = result.replace(element, entry.getValue());
                            modified++;
                        }
                    }
                } // end while
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") Exception exception) {
                logger.error("STRANGE ERROR inside SpecialCharParser: {}", result, exception);
            }
        } else {
            logger.error("STRANGE line null in specialCharParser, timeStamp={}", System.currentTimeMillis());
        }

        if (result != null) {
            result = new String(result);
        }

        return result;
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void closeStandardStreams() {
        closeObjects(System.in, System.out, System.err);
    }

    public static boolean checkAtomicBooleans(final AtomicBoolean... atomicBooleans) {
        return checkAtomicBooleans(true, atomicBooleans);
    }

    public static boolean checkAtomicBooleans(final boolean valueToCheck, @NotNull final AtomicBoolean... atomicBooleans) {
        boolean found = false;
        for (final AtomicBoolean atomicBoolean : atomicBooleans) {
            if (atomicBoolean.get() == valueToCheck) {
                found = true;
                break;
            }
        }
        return found;
    }

    public static boolean checkObjects(@NotNull final Object... objects) {
        boolean found = false;
        for (final Object object : objects) {
//            if (object == null) {
//                logger.error("null object in checkObjects: {}", objectToString(objects));
//            } else {
            final Class<?> clazz = object.getClass();
            if (clazz.equals(AtomicBoolean.class)) {
                final AtomicBoolean atomicBoolean = (AtomicBoolean) object;
                if (atomicBoolean.get()) {
                    found = true;
                    break;
                }
            } else if (clazz.equals(AtomicReference.class)) {
                final AtomicReference<?> atomicReference = (AtomicReference<?>) object;
                if (atomicReference.get() != null) {
                    found = true;
                    break;
                }
                // boolean support is useless and can be confusing; primitive boolean has a value that doesn't change
//                } else if (clazz.equals(Boolean.class)) {
//                    final Boolean boo = (Boolean) object;
//                    if (boo) {
//                        found = true;
//                        break;
//                    }
            } else {
                logger.error("unsupported class in checkObjects: {}", object.getClass());
            }
//            }
        }
        return found;
    }

    // public static void threadSleepSegmented(long totalSleepMillis, Object... atomicBooleans) {
    //     threadSleepSegmented(totalSleepMillis, 100L, atomicBooleans);
    // }
    public static boolean threadSleepSegmented(final long totalSleepMillis, final long segmentMillis, final Object... objects) {
        final boolean hasReachedEndOfSleep;
        if (totalSleepMillis > 0L && segmentMillis > 0L) {
            final long endTime = System.currentTimeMillis() + totalSleepMillis;
            @SuppressWarnings("NumericCastThatLosesPrecision") int segments = (int) (totalSleepMillis / segmentMillis);

            whileLoop:
            do {
                for (int i = 0; i < segments && i < 100; i++) {
                    if (checkObjects(objects)) {
                        break whileLoop;
                    }
                    threadSleep(segmentMillis);
                }
                final long leftTime = endTime - System.currentTimeMillis();

                if (leftTime <= 0) {
                    segments = 0;
                } else if (leftTime <= segmentMillis) {
                    if (checkObjects(objects)) {
                        break; // breaks from while
                    }
                    threadSleep(leftTime);
                    segments = 0;
                } else {
                    //noinspection NumericCastThatLosesPrecision
                    segments = (int) (leftTime / segmentMillis);
                }
            } while (segments > 0);
            hasReachedEndOfSleep = segments <= 0;
        } else if (totalSleepMillis <= 0L) { // this is actually acceptable, it just won't sleep at all
            hasReachedEndOfSleep = true;
        } else { // negative or zero segmentMillis
            logger.error("negative or zero segmentMillis in threadSleepSegmented for: {} {} {}", totalSleepMillis, segmentMillis, objectToString(objects));
            hasReachedEndOfSleep = false;
        }

        return hasReachedEndOfSleep;
    }

    @SuppressWarnings("SleepWhileInLoop")
    public static void threadSleep(final long millis) {
        if (millis > 0L) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException interruptedException) {
                logger.error("InterruptedException in threadSleep()", interruptedException);
            }
        } else { // nothing to be done
        }
    }

    // I can't make this work in Java 12
//    public static void setFinalStatic(Field field, Object newValue)
//            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
//        if (field != null) {
//            // to be used only in tests; it only works in some circumstances
//            final Field modifiersField = Field.class.getDeclaredField("modifiers");
////        final boolean fieldAccessible = field.isAccessible(), modifiersAccessible = modifiersField.isAccessible();
//            final int modifiersValue = field.getModifiers();
//
//            try {
////            if (!fieldAccessible) {
//                field.setAccessible(true);
////            }
////            if (!modifiersAccessible) {
//                modifiersField.setAccessible(true);
////            }
//                if (Modifier.isFinal(modifiersValue)) {
//                    modifiersField.setInt(field, modifiersValue & ~Modifier.FINAL);
//                }
//
//                field.set(null, newValue);
//            } finally {
//                if (field.getModifiers() != modifiersValue) {
//                    modifiersField.setInt(field, modifiersValue);
//                }
////            if (modifiersField.isAccessible() != modifiersAccessible) {
////                modifiersField.setAccessible(modifiersAccessible);
////            }
////            if (field.isAccessible() != fieldAccessible) {
////                field.setAccessible(fieldAccessible);
////            }
//                modifiersField.setAccessible(false);
//                field.setAccessible(false);
//            }
//        } else {
//            logger.error("null field argument in setFinalStatic {}", Generic.objectToString(newValue));
//        }
//    }

    public static Object getField(final Object object, final String fieldName) {
        Object result = null;

        if (object != null && fieldName != null) {
            final Class<?> clazz = object instanceof Class<?> ? (Class<?>) object : object.getClass();

            Field field = null;
//            boolean fieldAccessible = true;
            try {
                field = clazz.getDeclaredField(fieldName);
//                fieldAccessible = field.isAccessible();
//                if (!fieldAccessible) {
                field.setAccessible(true);
//                }

                result = field.get(object); // if static field, .get() argument is ignored
            } catch (NoSuchFieldException noSuchFieldException) {
                logger.error("NoSuchFieldException in getField: {}", new Object[]{clazz, fieldName}, noSuchFieldException);
            } catch (IllegalAccessException illegalAccessException) {
                logger.error("IllegalAccessException in getField: {}", new Object[]{clazz, fieldName}, illegalAccessException);
            } finally {
//                if (field != null && field.isAccessible() != fieldAccessible) {
//                    field.setAccessible(fieldAccessible);
//                }
                if (field != null) {
                    field.setAccessible(false);
                } else { // null field, nothing on this branch
                }
            }
        } else {
            logger.error("STRANGE object or fieldName null in getField, {} {} timeStamp={}", object, fieldName, System.currentTimeMillis());
        }

        return result;
    }

    public static boolean setField(final Object object, final String fieldName, final Object value) {
        boolean setSuccess = false;

        if (object != null && fieldName != null) {
            Field field = null;
//            boolean fieldAccessible = true;
            try {
                field = object.getClass().getDeclaredField(fieldName);
//                fieldAccessible = field.isAccessible();
//                if (!fieldAccessible) {
                field.setAccessible(true);
//                }
                field.set(object, value);

                setSuccess = true;
            } catch (NoSuchFieldException noSuchFieldException) {
                logger.error("NoSuchFieldException in setField: {}", new Object[]{object.getClass(), fieldName, value}, noSuchFieldException);
            } catch (IllegalAccessException illegalAccessException) {
                logger.error("IllegalAccessException in setField: {}", new Object[]{object.getClass(), fieldName, value}, illegalAccessException);
            } finally {
//                if (field != null && field.isAccessible() != fieldAccessible) {
//                    field.setAccessible(fieldAccessible);
//                }
                if (field != null) {
                    field.setAccessible(false);
                } else { // null field, nothing on this branch
                }
            }
        } else {
            logger.error("STRANGE object or fieldName null in setField, {} {} {} timeStamp={}", object, fieldName, value, System.currentTimeMillis());
        }

        return setSuccess;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static void turnOffHtmlUnitLogger() {
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.DefaultCssErrorHandler").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.html.InputElementFactory").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.javascript.host.dom.Document").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.javascript.background.DefaultJavaScriptExecutor").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.IncorrectnessListenerImpl").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.WebConsole").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.javascript.StrictErrorReporter").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.javascript.host.ActiveXObject").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.javascript.host.html.HTMLDocument").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.html.HtmlScript").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit.javascript.host.xml.XMLHttpRequest").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies").setLevel(java.util.logging.Level.OFF);
        final String NO_OP_LOG = "org.apache.commons.logging.impl.NoOpLog", DEFAULT_LOG = "org.apache.commons.logging.simplelog.defaultlog";
        System.setProperty("org.apache.commons.logging.Log", NO_OP_LOG);
        System.setProperty(DEFAULT_LOG, NO_OP_LOG);
        System.getProperties().setProperty(DEFAULT_LOG, "fatal");
    }
}
