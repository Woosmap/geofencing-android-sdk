package com.webgeoservices.woosmapgeofencing

import org.hamcrest.core.IsEqual
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.ojalgo.matrix.PrimitiveMatrix
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.HashMap

class FIGMMCreatorTest {
    val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss+SS")
    var visit1 = LoadedVisit()
    var visit2 = LoadedVisit()
    var visit3 = LoadedVisit()
    var visit4 = LoadedVisit()
    var visit_on_home1 = LoadedVisit()
    var visit_on_home2 = LoadedVisit()
    var figmmForVisitsCreator = FigmmForVisitsCreator()

    @Before
    fun setUp() {
        val start_date = formatter.parse("2018-01-01 12:00:35+00").time
        val end_date = start_date + (7*3600)*1000
        visit1 = LoadedVisit(2.0, 1.0, 15.0, "visit1", start_date,end_date)
        visit2 = LoadedVisit(6.0, 2.0, 20.0, "visit2", start_date + (24*3600)*1000, start_date + (24*3600 + 1800)*1000)
        visit3 = LoadedVisit(6.0, 2.0, 20.0, "visit3", start_date + (24*3600)*1000, start_date + (24*3600 + 1800)*1000)
        visit4 = LoadedVisit(6.0001, 2.0, 20.0, "visit4", start_date + (24*3600 + 3*3600)*1000, start_date + (24*3600 + 5*3600)*1000)

        visit_on_home1 = LoadedVisit(2.0, 1.0, 15.0, "visit_on_home1", start_date, end_date)
        visit_on_home2 = LoadedVisit(2.0, 1.0001, 20.0, "visit_on_home2", start_date + (19*3600)*1000, start_date + (23*3600)*1000)
    }

    @Test
    fun test_when_get_chi_squared_value_then_return_chi_squared_value(){
        val chi_value1 = 0.95
        val chi_value2 = 0.80
        val chi_value3 = 0.30

        val chi_value1_result  = figmmForVisitsCreator.chi_squared_value(1 - chi_value1)
        val chi_value2_result  = figmmForVisitsCreator.chi_squared_value(1 - chi_value2)
        val chi_value3_result  = figmmForVisitsCreator.chi_squared_value(1 - chi_value3)

        val chi_value1_result_2digit:Double = Math.round(chi_value1_result * 100.0) / 100.0
        val chi_value2_result_2digit:Double = Math.round(chi_value2_result * 100.0) / 100.0
        val chi_value3_result_2digit:Double = Math.round(chi_value3_result * 100.0) / 100.0

        Assert.assertThat(0.10, IsEqual.equalTo(chi_value1_result_2digit))
        Assert.assertThat(0.45, IsEqual.equalTo(chi_value2_result_2digit))
        Assert.assertThat(2.41, IsEqual.equalTo(chi_value3_result_2digit))
    }

    @Test
    fun test_when_update_zois_with_visits_without_zois_then_create_zois(){
        figmmForVisitsCreator.figmmAlgoTest(visit1.x,visit1.y, visit1.accuray.toFloat(),visit1.id,visit1.startime,visit1.endtime)

        var zoiToTest = figmmForVisitsCreator.list_zois.first()

        var covariance_matrix_inverse: PrimitiveMatrix = zoiToTest.get("covariance_matrix_inverse") as PrimitiveMatrix

        Assert.assertThat(covariance_matrix_inverse.get(0), IsEqual.equalTo(0.0011111111111111111))
        Assert.assertThat(covariance_matrix_inverse.get(1), IsEqual.equalTo(0.0))
        Assert.assertThat(covariance_matrix_inverse.get(2), IsEqual.equalTo(0.0))
        Assert.assertThat(covariance_matrix_inverse.get(3), IsEqual.equalTo(0.0011111111111111111))
        Assert.assertThat(810000.0, IsEqual.equalTo(zoiToTest["covariance_det"]))
        Assert.assertThat(1.0, IsEqual.equalTo(zoiToTest["prior_probability"]))
        Assert.assertThat(1.0, IsEqual.equalTo(zoiToTest["accumulator"]))
        Assert.assertThat(1.0, IsEqual.equalTo(zoiToTest["age"]))

        var mean:DoubleArray = zoiToTest["mean"] as DoubleArray
        Assert.assertThat(111319.49079327357, IsEqual.equalTo(mean[0]))
        Assert.assertThat(222684.20850554455, IsEqual.equalTo(mean[1]))

        var wktToTest = "POLYGON((1.0 2.000417934943155,0.9999478623048794 2.000414674076058,0.9998965382010534 2.000404942359496," +
                "0.9998468285839961 2.000388891653516,0.9997995091556561 2.0003667724238356,0.9997553183198861 2.000338929833409," +
                "0.9997149456598985 2.000305798356272,0.9996790211775493 2.0002678949977213,0.9996481054623703 2.0002258112267195," +
                "0.9996226809437566 2.000180203746093,0.9996031443628183 2.0001317842451045,0.9995898005813662 2.0000813082936255," +
                "0.9995828578246472 2.000029563551864,0.9995824244320577 1.9999773574791186,0.9995885071665441 1.999925504733684," +
                "0.9996010111090695 1.9998748144604137,0.9996197411397924 1.99982607766416,0.9996444049828481 1.9997800548665765," +
                "0.9996746177672146 1.999737464238271,0.9997099080324967 1.9996989703920343,0.999749725085908 1.9996651740117866," +
                "0.9997934475956469 1.999636602479027,0.9998403932865727 1.9996137016432622,0.9998898295868797 1.9995968288646317," +
                "0.9999409850596359 1.9995862474373651,0.9999930614407986 1.999582122481231,1.0000452460958569 1.999584518364842," +
                "1.000096724700722 1.9995933977011975,1.0001466939489831 1.999608621931125,1.0001943740872332 1.9996299534854338," +
                "1.0002390210828622 1.9996570594921306,1.0002799382344358 1.9996895169707747,1.0003164870434889 1.9997268194329436," +
                "1.00034809717808 1.999768384785924,1.0003742753726312 1.9998135644159298,1.0003946131251724 1.999861653309735," +
                "1.0004087930718768 1.99991190105597,1.0004165939394192 1.9999635235552247,1.00041789399787 2.000015715255565," +
                "1.000412672960253 2.0000676617229387,1.0004010122991127 2.000118552350175,1.000383093975167 2.0001675930061906," +
                "1.000359197597868 2.0002140184281303,1.000329696062191 2.0002571041630652,1.000295049729733 2.000296177872816," +
                "1.0002557992449246 2.0003306298255215,1.0002125570984535 2.000359922410268,1.0001659980695523 2.000383598526376," +
                "1.0001168486962955 2.0004012887161977,1.000065875938214 2.0004127169304624,1.0000138752081493 2.0004177048358125))"

        var wktFromZoi = figmmForVisitsCreator.figmm(zoiToTest)
        Assert.assertThat(wktFromZoi, IsEqual.equalTo(wktToTest))

        Assert.assertThat(visit1.startime, IsEqual.equalTo(zoiToTest["startTime"]))
        Assert.assertThat(visit1.endtime, IsEqual.equalTo(zoiToTest["endTime"]))


        figmmForVisitsCreator.figmmAlgoTest(visit2.x,visit2.y, visit2.accuray.toFloat(),visit2.id,visit2.startime,visit2.endtime)
        zoiToTest = figmmForVisitsCreator.list_zois.first()

        covariance_matrix_inverse = zoiToTest.get("covariance_matrix_inverse") as PrimitiveMatrix

        Assert.assertThat(covariance_matrix_inverse.get(0), IsEqual.equalTo(0.0011111111111111111))
        Assert.assertThat(covariance_matrix_inverse.get(1), IsEqual.equalTo(0.0))
        Assert.assertThat(covariance_matrix_inverse.get(2), IsEqual.equalTo(0.0))
        Assert.assertThat(covariance_matrix_inverse.get(3), IsEqual.equalTo(0.0011111111111111111))
        Assert.assertThat(810000.0, IsEqual.equalTo(zoiToTest["covariance_det"]))
        Assert.assertThat(0.5, IsEqual.equalTo(zoiToTest["prior_probability"]))
        Assert.assertThat(1.0, IsEqual.equalTo(zoiToTest["accumulator"]))
        Assert.assertThat(1.0, IsEqual.equalTo(zoiToTest["age"]))

        mean = zoiToTest["mean"] as DoubleArray
        Assert.assertThat(111319.49079327357, IsEqual.equalTo(mean[0]))
        Assert.assertThat(222684.20850554455, IsEqual.equalTo(mean[1]))

        wktToTest = "POLYGON((1.0 2.000417934943155,0.9999478623048794 2.000414674076058,0.9998965382010534 2.000404942359496,0.9998468285839961 " +
                "2.000388891653516,0.9997995091556561 2.0003667724238356,0.9997553183198861 2.000338929833409,0.9997149456598985 2.000305798356272," +
                "0.9996790211775493 2.0002678949977213,0.9996481054623703 2.0002258112267195,0.9996226809437566 2.000180203746093,0.9996031443628183 " +
                "2.0001317842451045,0.9995898005813662 2.0000813082936255,0.9995828578246472 2.000029563551864,0.9995824244320577 1.9999773574791186," +
                "0.9995885071665441 1.999925504733684,0.9996010111090695 1.9998748144604137,0.9996197411397924 1.99982607766416,0.9996444049828481 " +
                "1.9997800548665765,0.9996746177672146 1.999737464238271,0.9997099080324967 1.9996989703920343,0.999749725085908 1.9996651740117866," +
                "0.9997934475956469 1.999636602479027,0.9998403932865727 1.9996137016432622,0.9998898295868797 1.9995968288646317,0.9999409850596359 " +
                "1.9995862474373651,0.9999930614407986 1.999582122481231,1.0000452460958569 1.999584518364842,1.000096724700722 1.9995933977011975," +
                "1.0001466939489831 1.999608621931125,1.0001943740872332 1.9996299534854338,1.0002390210828622 1.9996570594921306,1.0002799382344358 " +
                "1.9996895169707747,1.0003164870434889 1.9997268194329436,1.00034809717808 1.999768384785924,1.0003742753726312 1.9998135644159298," +
                "1.0003946131251724 1.999861653309735,1.0004087930718768 1.99991190105597,1.0004165939394192 1.9999635235552247,1.00041789399787 " +
                "2.000015715255565,1.000412672960253 2.0000676617229387,1.0004010122991127 2.000118552350175,1.000383093975167 2.0001675930061906," +
                "1.000359197597868 2.0002140184281303,1.000329696062191 2.0002571041630652,1.000295049729733 2.000296177872816,1.0002557992449246 " +
                "2.0003306298255215,1.0002125570984535 2.000359922410268,1.0001659980695523 2.000383598526376,1.0001168486962955 2.0004012887161977," +
                "1.000065875938214 2.0004127169304624,1.0000138752081493 2.0004177048358125))"
        wktFromZoi = figmmForVisitsCreator.figmm(zoiToTest)
        Assert.assertThat(wktFromZoi, IsEqual.equalTo(wktToTest))

        Assert.assertThat(visit1.startime, IsEqual.equalTo(zoiToTest["startTime"]))
        Assert.assertThat(visit1.endtime, IsEqual.equalTo(zoiToTest["endTime"]))

        zoiToTest = figmmForVisitsCreator.list_zois.last()

        covariance_matrix_inverse = zoiToTest.get("covariance_matrix_inverse") as PrimitiveMatrix

        Assert.assertThat(covariance_matrix_inverse.get(0), IsEqual.equalTo(0.000625))
        Assert.assertThat(covariance_matrix_inverse.get(1), IsEqual.equalTo(0.0))
        Assert.assertThat(covariance_matrix_inverse.get(2), IsEqual.equalTo(0.0))
        Assert.assertThat(covariance_matrix_inverse.get(3), IsEqual.equalTo(0.000625))
        Assert.assertThat(2560000.0, IsEqual.equalTo(zoiToTest["covariance_det"]))
        Assert.assertThat(0.5, IsEqual.equalTo(zoiToTest["prior_probability"]))
        Assert.assertThat(1.0, IsEqual.equalTo(zoiToTest["accumulator"]))
        Assert.assertThat(1.0, IsEqual.equalTo(zoiToTest["age"]))

        mean = zoiToTest["mean"] as DoubleArray
        Assert.assertThat(222638.98158654713, IsEqual.equalTo(mean[0]))
        Assert.assertThat(669141.0570442454, IsEqual.equalTo(mean[1]))

        wktToTest = "POLYGON((2.0 6.00055453153006,1.9999304830731726 6.0005502048927015,1.9998620509347376 6.000537292496194,1.999795771445328 " +
                "6.00051599583355,1.9997326788742078 6.000486647231093,1.9996737577598482 6.000449704662719,1.9996199275465314 6.000405744603353," +
                "1.9995720282367324 6.000355453033425,1.9995308072831603 6.000299614734423,1.9994969079250087 6.000239101042761,1.999470859150424 " +
                "6.000174856252975,1.9994530674418216 6.000107882882374,1.9994438104328627 6.0000392260272095,1.999443232576077 5.999969957054311," +
                "1.999451342888726 5.9999011568827925,1.9994680148120925 5.999833899116712,1.9994929881863897 5.999769233291776,1.9995258733104642 " +
                "5.999708168497643,1.9995661570229524 5.999651657631386,1.9996132107099955 5.999600582527665,1.9996663001145438 5.999555740198091," +
                "1.999724596794196 5.99951783039379,1.9997871910487637 5.999487444686075,1.9998531061158396 5.999465057235079,1.9999213134128475 " +
                "5.999451017390459,1.9999907485877313 5.999445544239955,2.000060328127809 5.999448723190505,2.000128966267629 5.999460504635506," +
                "2.0001955919319774 5.999480704728898,2.000259165449644 5.999509008254062,2.0003186947771496 5.999544973542725,2.0003732509792473 " +
                "5.999588039367145,2.000421982724652 5.999637533697939,2.0004641295707732 5.9996926841909985,2.000499033830175 5.999752630239729," +
                "2.000526150833563 5.99981643640469,2.000545057429169 5.999883107010822,2.0005554585858922 5.999951601684771,2.00055719199716 " +
                "6.000020851589583,2.0005502306136704 6.000089776103551,2.0005346830654838 6.000157299683038,2.000510791966889 6.000222368645863," +
                "2.0004789301304906 6.000283967613683,2.000439594749588 6.000341135356553,2.000393399639644 6.000392979792483,2.0003410656598994 " +
                "6.000438691908002,2.0002834094646045 6.000477558382479,2.0002213307594032 6.0005089727190954,2.0001557982617273 6.000532444708959," +
                "2.000087834584285 6.000547608080551,2.0000185002775326 6.000554226215257))"

        wktFromZoi = figmmForVisitsCreator.figmm(zoiToTest)
        Assert.assertThat(wktFromZoi, IsEqual.equalTo(wktToTest))

        Assert.assertThat(visit2.startime, IsEqual.equalTo(zoiToTest["startTime"]))
        Assert.assertThat(visit2.endtime, IsEqual.equalTo(zoiToTest["endTime"]))
    }

    @Test
    fun test_when_update_zois_prior_then_update_zois_prior_values() {
        var listZoiToTest: MutableList<Map<*, *>> = ArrayList()
        val zoiToTest1 = HashMap<String, Any>()
        val zoiToTest2 = HashMap<String, Any>()
        val zoiToTest3 = HashMap<String, Any>()

        zoiToTest1["prior_probability"] = 1/3
        zoiToTest2["prior_probability"] = 1/3
        zoiToTest3["prior_probability"] = 1/3

        zoiToTest1["accumulator"] = 2.0
        zoiToTest2["accumulator"] = 1.0
        zoiToTest3["accumulator"] = 3.0

        listZoiToTest.add(zoiToTest1)
        listZoiToTest.add(zoiToTest2)
        listZoiToTest.add(zoiToTest3)

        figmmForVisitsCreator.list_zois = listZoiToTest

        figmmForVisitsCreator.update_zois_prior()

        Assert.assertThat(0.3333333333333333, IsEqual.equalTo(zoiToTest1["prior_probability"]))
        Assert.assertThat(0.16666666666666666, IsEqual.equalTo(zoiToTest2["prior_probability"]))
        Assert.assertThat(0.5, IsEqual.equalTo(zoiToTest3["prior_probability"]))
    }

    @Test
    fun test_when_update_zois_with_a_visit_near_an_existing_cluster_then_update_and_requalify_zois(){
        figmmForVisitsCreator.figmmAlgoTest(visit_on_home1.x,visit_on_home1.y, visit_on_home1.accuray.toFloat(),visit_on_home1.id,visit_on_home1.startime,visit_on_home1.endtime)
        figmmForVisitsCreator.figmmAlgoTest(visit_on_home2.x,visit_on_home2.y, visit_on_home2.accuray.toFloat(),visit_on_home2.id,visit_on_home2.startime,visit_on_home2.endtime)
        figmmForVisitsCreator.figmmAlgoTest(visit3.x,visit3.y, visit3.accuray.toFloat(),visit3.id,visit3.startime,visit3.endtime)

        Assert.assertThat(2, IsEqual.equalTo(figmmForVisitsCreator.list_zois.size))

        var zoi_before_update = HashMap<String, Any>()

        for (zoi in figmmForVisitsCreator.list_zois){
            var idList:ArrayList<String> = zoi["idVisits"] as ArrayList<String>
            for (id in idList) {
                if (id == visit3.id) {
                    zoi_before_update = zoi as HashMap<String, Any>
                }
            }
        }

        Assert.assertThat(visit3.startime, IsEqual.equalTo(zoi_before_update["startTime"]))
        Assert.assertThat(visit3.endtime, IsEqual.equalTo(zoi_before_update["endTime"]))

        figmmForVisitsCreator.figmmAlgoTest(visit4.x,visit4.y, visit4.accuray.toFloat(),visit4.id,visit4.startime,visit4.endtime)

        Assert.assertThat(2, IsEqual.equalTo(figmmForVisitsCreator.list_zois.size))

        var zoi_after_update = HashMap<String, Any>()

        for (zoi in figmmForVisitsCreator.list_zois){
            var idList:ArrayList<String> = zoi["idVisits"] as ArrayList<String>
            for (id in idList) {
                if (id == visit4.id) {
                    zoi_after_update = zoi as HashMap<String, Any>
                }
            }
        }

        val covariance_matrix_inverse = zoi_after_update.get("covariance_matrix_inverse") as PrimitiveMatrix

        Assert.assertThat(covariance_matrix_inverse.get(0), IsEqual.equalTo(0.00125))
        Assert.assertThat(covariance_matrix_inverse.get(1), IsEqual.equalTo(0.0))
        Assert.assertThat(covariance_matrix_inverse.get(2), IsEqual.equalTo(0.0))
        Assert.assertThat(covariance_matrix_inverse.get(3), IsEqual.equalTo(0.0012749591681057728))
        Assert.assertThat(639056.2259371291, IsEqual.equalTo(zoi_after_update["covariance_det"]))
        Assert.assertThat(0.5, IsEqual.equalTo(zoi_after_update["prior_probability"]))

        Assert.assertThat(2.0, IsEqual.equalTo(zoi_after_update["accumulator"]))
        Assert.assertThat(2.0, IsEqual.equalTo(zoi_after_update["age"]))

        val mean = zoi_after_update["mean"] as DoubleArray
        Assert.assertThat(222638.98158654713, IsEqual.equalTo(mean[0]))
        Assert.assertThat(669146.6536782421, IsEqual.equalTo(mean[1]))
    }

}

