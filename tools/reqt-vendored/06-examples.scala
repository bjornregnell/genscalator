package reqt
package examples

import scala.util.Failure
import scala.util.Success


val menu: Map[String, Model] = Map(
  "Context 1: Simple" -> Lauesen.ContextDiagramSimple,
  "Context 2: Interfaces with Data" -> Lauesen.ContextDiagramInterfaces,
  "Data 1: Classes with Relations" -> Lauesen.DataRelations,
  "Data 2: Classes with Fields" -> Lauesen.DataEntities,
  "Multi-level 1: Goal-Design Scale" -> Lauesen.GoalDesignScale,
  "Multi-level 2: Why-Spec-Example" -> Lauesen.WhySpecExample,
  "Multi-level 3: Model with Sections" -> Lauesen.ModelWithSections,
  "Prioritization: 100$ test" -> Prioritization.DollarTest,
  "Quality 1: System Quality Aspects" -> Lauesen.QualityRequirements,
  "Quality 2: Quper Model" -> QualityModel.StartupQuality,
  "Release Planning 1: small problem" -> constraintProblems.releasePlanSimple,
  "Release Planning 2: large problem" -> constraintProblems.releasePlanAdvanced,
  "Scenarios: Hotel Reception Tasks" -> Lauesen.TaskHotelReceptionWork,
  "State Machine: Room State Model" -> Lauesen.StateMachine,
  "Variability Model: Color" -> VariabilityModel.Appearance,
)

object constraintProblems:
  val releasePlanSimple = Model(
      Stakeholder("s1") has (
        Prio(1),
        Feature("f1") has Benefit(4),
        Feature("f2") has Benefit(2),
        Feature("f3") has Benefit(1),
      ),
      Stakeholder("s2") has (
        Prio(2),
        Feature("f1") has Benefit(2),
        Feature("f2") has Benefit(1),
        Feature("f3") has Benefit(1),
      ),
      Release("r1") precedes Release("r2"),
      Resource("dev") has (
        Feature("f1") has Cost(10),
        Feature("f2") has Cost(70),
        Feature("f3") has Cost(40),
        Release("r1") has Capacity(100),
        Release("r2") has Capacity(100),
      ),
      Resource("test") has (
        Feature("f1") has Cost(40),
        Feature("f2") has Cost(10),
        Feature("f3") has Cost(70),
        Release("r1") has Capacity(100),
        Release("r2") has Capacity(100),
      ),
      Feature("f3") precedes Feature("f1")
    ) 
  
  val releasePlanAdvanced = Model(
    Feature("exportHtml") has Gist("Export model to HTML, with special treatment of Section and Image."),
    Feature("exportGraphViz") has Gist("Export model to graph for visualization in GraphViz."),
    Feature("exportTabular") has Gist("Export model to table format for edit in spreadsheet apps."),
    Feature("exportLatex") has Gist("Export model to Latex, with special treatment of Section."),
    Feature("exportContexDiagramSvg") has Gist("Solve release planning problems."),
    Feature("syntaxColoring") has Gist("Syntax colored editing of models."),
    Feature("releasePlanning") has Gist("Constraint-solving support and gui for release planning."),
    Feature("autoCompletion") has Gist("Auto-completion of entity, attribute and relation types."),
    Feature("autoSave") has Gist("Save a model automatically after each update."),
    Feature("exportHtml") precedes Feature("exportGraphViz"),
    Resource("TeamA") has (
      Feature("exportHtml") has Cost(9),
      Feature("exportGraphViz") has Cost(7),
      Feature("exportTabular") has Cost(3),
      Feature("exportLatex") has Cost(6),
      Feature("exportContexDiagramSvg") has Cost(3),
      Feature("syntaxColoring") has Cost(6),
      Feature("autoCompletion") has Cost(3),
      Feature("releasePlanning") has Cost(4),
      Feature("autoSave") has Cost(6),
      Release("March") has Capacity(20),
      Release("September") has Capacity(15),
      Release("zzzLater") has Capacity(1000),
    ),
    Resource("TeamB") has (
      Feature("exportHtml") has Cost(2),
      Feature("exportGraphViz") has Cost(8),
      Feature("exportTabular") has Cost(9),
      Feature("exportLatex") has Cost(4),
      Feature("exportContexDiagramSvg") has Cost(4),
      Feature("syntaxColoring") has Cost(2),
      Feature("autoCompletion") has Cost(3),
      Feature("releasePlanning") has Cost(5),
      Feature("autoSave") has Cost(7),
      Release("March") has Capacity(15),
      Release("September") has Capacity(15),
      Release("zzzLater") has Capacity(1000),
    ),
    Release("March") precedes Release("September"),
    Release("September") precedes Release("zzzLater"),
    Stakeholder("Ada") has (Prio(1),
      Feature("exportHtml") has Benefit(10),
      Feature("exportGraphViz") has Benefit(10),
      Feature("exportTabular") has Benefit(10),
      Feature("exportLatex") has Benefit(7),
      Feature("exportContexDiagramSvg") has Benefit(6),
      Feature("syntaxColoring") has Benefit(3),
      Feature("releasePlanning") has Benefit(4),
      Feature("autoCompletion") has Benefit(7),
      Feature("autoSave") has Benefit(9),
    ),
    Stakeholder("Ben") has (
      Prio(1),
      Feature("exportHtml") has Benefit(1),
      Feature("exportGraphViz") has Benefit(9),
      Feature("exportTabular") has Benefit(3),
      Feature("exportLatex") has Benefit(4),
      Feature("exportContexDiagramSvg") has Benefit(7),
      Feature("syntaxColoring") has Benefit(8),
      Feature("releasePlanning") has Benefit(5),
      Feature("autoCompletion") has Benefit(10),
      Feature("autoSave") has Benefit(4),
    ),
  )

/** Examples from "Software Requirements - Styles and techniques" by S. Lauesen (2002)". */
object Lauesen:
  val allExamples = 
    Seq(GoalDesignScale, WhySpecExample, ContextDiagramSimple, ContextDiagramInterfaces, DataRelations, DataEntities, StateMachine, QualityRequirements, ModelWithSections, TaskHotelReceptionWork)

  val GoalDesignScale = Model(
    Title("Goal-Domain-Product-Design example"),
    Goal("accuracy").has(
      Spec("Our pre-calculations shall hit within 5%"),
    ),
    Feature("quotation").has(
      Spec("Product shall support cost recording and quotation with experience data"),
    ),
    Function("experienceData").has(
      Spec("Product shall have recording and retrieval functions for experience data"),
    ),
    Design("screenX").has(
      Spec("System shall have screen pictures as shown in Fig. X"),
    ),
  )

  val WhySpecExample = Model(
    Title("Why-Spec-Example"),
    Feature("navigate").has(
      Why("Measuring neural response is a bit painful to the  patient. Electrodes must be kept in place ... So both hands should be at the patient during a measurement."),
      Spec("It shall be possible to perform the commands start, stop, ... with both hands at the patient."),
      Example("Might be done with mini keyboard (wrist keys), foot pedal, voice recognition, etc."),
    ),
  )

  val ContextDiagramSimple = Model(
    Section("context").has(
      Product("hotelApp").interactsWith(
        User("receptionist"),
        User("guest"),
        System("accounting"),
        System("telephony"),
      ),
    ),
  )

  val ContextDiagramInterfaces = Model(
    Product("hotelApp").has(
      Interface("receptionUI").has(
        User("Receptionist"),
        Data("receptionIO").has(
          Input("booking"),
          Input("checkOut"),
          Output("serviceNote"),
        )
      ),
      Interface("guestUI").has(
        User("Guest"),
        Data("guestIO").has(
          Output("confirmation"),
          Output("invoice"),
        )
      ),
      Interface("phoneAPI").has(
        System("Telephony"),
      ),
      Interface("accountAPI").has(
        System("Accounting"),
      ),
    ),
  )

  val DataRelations = Model(
      Class("Guest").relatesTo(
        Class("Stay"),
        Min(1),
      ),
      Class("Stay").relatesTo(
        Class("RoomState"),
        Class("RoomService"),
        Min(1),
      ),
      Class("RoomServiceType").relatesTo(
        Class("RoomService"),
        Min(1),
      ),
      Class("Room").relatesTo(
        Class("RoomState"),
        Min(1),
      ),
  )

  val DataEntities = Model(
    Title("Data Entities"),
    Class("Guest").has(
      Field("name"),
      Field("address1"),
      Field("address2"),
      Field("address3"),
      Field("passport"),
    ),
    Class("Stay").has(
      Field("stayId"),
      Field("paymethod"),
      Field("employee"),
    ),
    Class("RoomServiceType").has(
      Field("name"),
      Field("price"),
    ),
    Class("RoomService").has(
      Field("serviceDate"),
      Field("serviceCount"),
    ),
    Class("Room").has(
      Field("roomId"),
      Field("bedCount"),
      Field("roomType"),
      Field("price1"),
      Field("price2"),
    ),
    Class("RoomState").has(
      Field("date"),
      Field("personCount"),
      Field("state"),
    ),
  )

  val StateMachine = Model(
    Title("Room State Model"),
    Section("roomState").has(
      State("free").has(
        Event("book").precedes(
          State("booked"),
        ),
        Event("checkin").precedes(
          State("occupied"),
        ),
        Event("changeRoom").precedes(
          State("occupied"),
        ),
        Event("repair").precedes(
          State("repairing"),
        ),
      ),
      State("booked").has(
        Event("checkIn").precedes(
          State("occupied"),
        ),
        Event("cancel").precedes(
          State("free"),
        ),
      ),
      State("occupied").has(
        Event("checkout").precedes(
          State("free"),
        ),
        Event("changeRoom").precedes(
          State("free"),
        ),
      ),
      State("repairing").has(
        Event("done").precedes(
          State("free"),
        ),
      ),
    ),
  )

  val QualityRequirements = Model(
    Section("quality").has(
      Text("This section contains system-wide quality requirements."),
      Quality("databaseCapacity").has(
        Spec("#guests < 10,000 growing 20% per year, #rooms < 1,000"),
      ),
      Quality("calendarAccuracy").has(
        Spec("Bookings shall be possible at least two years ahead."),
      ),
      Quality("forecastPerformance").has(
        Spec("Product shall compute a room occupation forecast within ___ minutes. (Customer expects one minute.)"),
      ),
      Quality("taskUsability").has(
        Spec("Novice users shall perform tasks Q and R in 15 minutes. Experienced users shall perform tasks Q, R, S in 2 minutes."),
      ),
      Quality("taskUsability").relatesTo(
        Task("Q"),
        Task("R"),
        Task("S"),
      ),
      Quality("peakLoadPerformance").has(
        Spec("Product shall be able to process 100 payment transactions per second in peak load."),
      ),
    ),
  )

  val ModelWithSections = Model(
    Title("Test Model"),
    Text("This is a model to test html generation."),
    Feature("topStuff") has Spec("Hello top-level stuff."),
    Feature("deepTopStuff") has (Feature("Gurka") has Spec("hejsan")),
    Section("context") has (
      Text("This section describes the context of the system."),
      Image("ctxImg") has Location("context-diagram.png"),
      Product("hotelApp") implements (
        Interface("receptionUI") has User("receptionist"),
        Interface("guestUI") has User("guest"),
        Interface("phoneAPI") requires System("telephony"),
        Interface("accountAPI") requires System("accounting")),
      Interface("receptionUI") has (
        Input("booking"), Input("checkOut"),
        Output("serviceNote")),
      Interface("guestUI") has (
        Output("confirmation"), Output("invoice"))
    )
  ) ++ QualityRequirements

  val TaskHotelReceptionWork = Model(
    Task("receptionWork") has (
      Task("booking"),
      Task("checkIn") has (
        Why("Guest wants room."),
        Frequency(3),
        Spec("Give guest a room, mark it as occupied and start account. Frequency scale is median number of check-ins/room/week. Trigger: A guest arrives. Critical: Group tour with 50 guests."),
        Task("findRoom"),
        Task("recordGuest") has
          Spec("variants: a) Guest has booked in advance, b) No suitable room"),
        Task("deliverKey"))))

object Prioritization:
  val DollarTest = Model(
    Stakeholder("a").has(
      Prio(2),
      Req("r1") has Benefit(5),
      Req("r2") has Benefit(300),
      Req("r3") has Benefit(8),
      Req("r4") has Benefit(9),
      Req("r5") has Benefit(100),
      Req("r6") has Benefit(10),
      Req("r7") has Benefit(20)),
    Stakeholder("b").has(
      Prio(4),
      Req("r1") has Benefit(100),
      Req("r2") has Benefit(7),
      Req("r3") has Benefit(20),
      Req("r4") has Benefit(80),
      Req("r5") has Benefit(10),
      Req("r6") has Benefit(90),
      Req("r7") has Benefit(20)))

  def normalizedVotes(
    m: Model,
    voterType: EntType = Stakeholder,
    voterPrioType: IntAttrType = Prio, 
    requirementType: EntType = Req, 
    verdictType: IntAttrType = Benefit,
  ): Model =
    val voters: Vector[Ent] = m.ents.filter(_.t == voterType).distinct
    val requirements: Vector[Ent] = m.ents.filter(_.t == requirementType).distinct
    val prioSum: Int = voters.flatMap(s => m/s.has/voterPrioType).sum
    
    val benefitSum: Map[Ent, Int] = voters.map: s => 
        s -> (m/s.has).intAttrs.collect{ case IntAttr(verdict, b) => b}.sum
      .toMap

    util.Try:  // unsafe call to head below
      val relations: Vector[Rel] = requirements.map: r =>
        val result: Vector[Double] = voters.map: s =>
          val weighted = 
            (m/s.has/voterPrioType).head * (m/s.has/r.has/verdictType).head * 100.0 
          val tot = benefitSum(s) * prioSum
          weighted / tot
        val verdict = math.round(result.sum).toInt
        r has verdictType(verdict)
      relations
    match 
      case Success(relations) => Model(Section("NormalizedVotes").has(relations*))
      case Failure(exception) => 
        Model(Section("NormalizedVotes").has(
          StrAttrType.Failure(
            s"""|Failed to normalize as model does not conform to this assumed shape: 
                |__* $voterType: a has 
                |____* $voterPrioType: 1 
                |____* $requirementType: x has $verdictType: 1""".stripMargin)))

object QualityModel:
  val StartupQuality = Model(
    Quality("mtts") has (
      Gist("Mean time to startup"),
      Spec("Measured in milliseconds using Test startup"),
      Breakpoint("Utility") has Value(4000),
      Breakpoint("Differentiation") has Value(1500),
      Breakpoint("Saturation") has Value(200),
      Target("basic") has (
          Value(2000),
          Comment("Probably possible with existing architecture.")),
      Target("strech") has (
          Value(1100),
          Comment("Probably needs new architecture.")),
      Barrier("first") has Value(2100),
      Barrier("second") has Value(1000),
      Product("competitorX") has Value(2000),
      Product("competitorY") has Value(3000)
    ),
    Test("startup") verifies Quality("mtts"),
    Test("startup") has (
      Spec("Calculate average time in milliseconds of the startup time over 10  executions from start button is pressed to logon screen is shown."),
      Target("stretch")
    )
  )

object VariabilityModel:
  val Appearance = Model(
  Component("appearance") has (
    VariationPoint("color") has (
      Min(0), Max(2),
      Variant("blue"), Variant("red"), Variant("green")),
    VariationPoint("shape") has (
      Min(1), Max(1), Variant("round"), Variant("square")),
    VariationPoint("payment") has (
      Min(1), Max(2), Variant("cash"), Variant("credit")),
    VariationPoint("payment") requires Variant("cash"), /* mandatory */
    Variant("round") excludes Variant("red"),
    Variant("green") requires Variant("square")),
  Component("appearance") requires VariationPoint("shape"), /* mandatory */
  Product("free") requires Component("appearance"),
  Product("free") binds (
    VariationPoint("shape") binds Variant("round")),
  Product("premium") requires Component("appearance"),
  Product("premium") binds ( /* violating variability constraints */
    VariationPoint("color") binds (Variant("red"), Variant("green")),
    VariationPoint("shape") binds (Variant("round"), Variant("square")),
    VariationPoint("payment") binds Variant("cash")))



