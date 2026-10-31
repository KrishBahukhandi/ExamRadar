package com.examradar.app.data.study

/**
 * Official exam pattern specifications, marking rules, and historical cutoff benchmarks.
 */
data class ExamStagePattern(
    val stageName: String,               // e.g. "Prelims (Paper 1 - GS)"
    val examMode: String,                // e.g. "Offline (OMR Pen & Paper)" or "Online (Computer Based Test)"
    val questionCount: Int,
    val maxMarks: Int,
    val durationMinutes: Int,
    val negativeMarking: String,          // e.g. "-0.66 marks (1/3rd penalty)"
    val qualifyingCriteria: String       // e.g. "Counted for Merit (Expected Cutoff ~88-92)"
)

data class CutoffTrend(
    val year: String,                    // e.g. "2023"
    val generalCutoff: String,           // e.g. "75.41"
    val obcCutoff: String,               // e.g. "74.75"
    val ewsCutoff: String,               // e.g. "68.02"
    val scCutoff: String,                // e.g. "59.25"
    val stCutoff: String                 // e.g. "47.82"
)

data class ExamPatternGuide(
    val examId: String,
    val examName: String,
    val totalStages: Int,
    val stages: List<ExamStagePattern>,
    val cutoffTrends: List<CutoffTrend>,
    val strategyTip: String
)

object ExamPatternRepository {

    private val guides = mapOf(
        "exam-upsc-cse" to ExamPatternGuide(
            examId = "exam-upsc-cse",
            examName = "UPSC Civil Services Examination",
            totalStages = 3,
            stages = listOf(
                ExamStagePattern(
                    stageName = "Prelims Paper 1 (General Studies)",
                    examMode = "Offline (OMR)",
                    questionCount = 100,
                    maxMarks = 200,
                    durationMinutes = 120,
                    negativeMarking = "0.66 marks deduction (1/3rd of 2 marks)",
                    qualifyingCriteria = "Counted for Mains qualification (~85-92 Cutoff)"
                ),
                ExamStagePattern(
                    stageName = "Prelims Paper 2 (CSAT Aptitude)",
                    examMode = "Offline (OMR)",
                    questionCount = 80,
                    maxMarks = 200,
                    durationMinutes = 120,
                    negativeMarking = "0.83 marks deduction (1/3rd of 2.5 marks)",
                    qualifyingCriteria = "Qualifying Nature: Minimum 33% (66 Marks) Mandatory"
                ),
                ExamStagePattern(
                    stageName = "Mains Written Examination (9 Papers)",
                    examMode = "Conventional Written",
                    questionCount = 180,
                    maxMarks = 1750,
                    durationMinutes = 1620,
                    negativeMarking = "No negative marking (Subjective evaluation)",
                    qualifyingCriteria = "Counted towards final rank along with Interview"
                ),
                ExamStagePattern(
                    stageName = "Personality Test (Interview)",
                    examMode = "In-Person Board Panel",
                    questionCount = 0,
                    maxMarks = 275,
                    durationMinutes = 35,
                    negativeMarking = "N/A",
                    qualifyingCriteria = "Final merit calculated out of 2025 total marks"
                )
            ),
            cutoffTrends = listOf(
                CutoffTrend("2023", "75.41", "74.75", "68.02", "59.25", "47.82"),
                CutoffTrend("2022", "88.22", "87.54", "82.83", "74.08", "69.35"),
                CutoffTrend("2021", "87.54", "84.85", "80.14", "75.41", "70.71")
            ),
            strategyTip = "Focus on CSAT practice alongside GS-1; CSAT failure rates have climbed past 25% among top scorers. Prioritize high-weightage areas: Polity, Modern History & Economy."
        ),
        "exam-ssc-cgl" to ExamPatternGuide(
            examId = "exam-ssc-cgl",
            examName = "SSC Combined Graduate Level",
            totalStages = 2,
            stages = listOf(
                ExamStagePattern(
                    stageName = "Tier 1 (Computer Based Examination)",
                    examMode = "Online (CBT)",
                    questionCount = 100,
                    maxMarks = 200,
                    durationMinutes = 60,
                    negativeMarking = "0.50 marks deduction for each wrong answer",
                    qualifyingCriteria = "Qualifying in nature to shortlist candidates for Tier 2"
                ),
                ExamStagePattern(
                    stageName = "Tier 2 Paper 1 (Maths + Reasoning + Eng + GA + Computer)",
                    examMode = "Online (CBT) + Typing Test",
                    questionCount = 150,
                    maxMarks = 390,
                    durationMinutes = 150,
                    negativeMarking = "1 mark deduction for each incorrect response",
                    qualifyingCriteria = "Merit score decides post allocation (ASO, Excise, Income Tax)"
                )
            ),
            cutoffTrends = listOf(
                CutoffTrend("2023", "150.04", "145.93", "143.44", "126.69", "118.16"),
                CutoffTrend("2022", "114.27", "114.27", "102.35", "89.08", "77.56"),
                CutoffTrend("2021", "130.18", "117.87", "109.64", "94.58", "81.52")
            ),
            strategyTip = "Accuracy is paramount in Tier 1 with 0.50 negative marking and 60 minutes for 100 questions. Focus daily on calculation speed and previous year vocab repetitions."
        ),
        "exam-ibps-po" to ExamPatternGuide(
            examId = "exam-ibps-po",
            examName = "IBPS CRP Probationary Officer",
            totalStages = 3,
            stages = listOf(
                ExamStagePattern(
                    stageName = "Preliminary Examination",
                    examMode = "Online (CBT)",
                    questionCount = 100,
                    maxMarks = 100,
                    durationMinutes = 60,
                    negativeMarking = "0.25 marks penalty for each wrong answer",
                    qualifyingCriteria = "Sectional cutoffs in all 3 sections + overall cutoff required"
                ),
                ExamStagePattern(
                    stageName = "Main Examination (Objective + Descriptive)",
                    examMode = "Online (CBT)",
                    questionCount = 157,
                    maxMarks = 225,
                    durationMinutes = 210,
                    negativeMarking = "0.25 marks deduction of marks assigned to question",
                    qualifyingCriteria = "Weightage ratio of Main Exam to Interview is 80:20"
                ),
                ExamStagePattern(
                    stageName = "Common Interview",
                    examMode = "Bank Panel Interview",
                    questionCount = 0,
                    maxMarks = 100,
                    durationMinutes = 25,
                    negativeMarking = "N/A",
                    qualifyingCriteria = "Minimum qualifying marks: 40% (35% for SC/ST/OBC)"
                )
            ),
            cutoffTrends = listOf(
                CutoffTrend("2023", "54.25", "54.25", "54.25", "49.00", "43.00"),
                CutoffTrend("2022", "49.75", "49.75", "49.75", "46.75", "40.50"),
                CutoffTrend("2021", "50.50", "50.50", "50.50", "44.50", "38.00")
            ),
            strategyTip = "Clear sectional cutoffs first before attempting risky guesses. Puzzles and Data Interpretation require structured daily 2-hour timed sets."
        ),
        "exam-rrb-ntpc" to ExamPatternGuide(
            examId = "exam-rrb-ntpc",
            examName = "Railway NTPC (Graduate & Under Graduate)",
            totalStages = 3,
            stages = listOf(
                ExamStagePattern(
                    stageName = "1st Stage Computer Based Test (CBT-1)",
                    examMode = "Online (CBT)",
                    questionCount = 100,
                    maxMarks = 100,
                    durationMinutes = 90,
                    negativeMarking = "1/3rd (0.33) marks deduction for wrong response",
                    qualifyingCriteria = "Screening nature to shortlist 20 times the vacancies for CBT-2"
                ),
                ExamStagePattern(
                    stageName = "2nd Stage Computer Based Test (CBT-2)",
                    examMode = "Online (CBT)",
                    questionCount = 120,
                    maxMarks = 120,
                    durationMinutes = 90,
                    negativeMarking = "1/3rd (0.33) marks deduction",
                    qualifyingCriteria = "Normalized marks determine final selection merit"
                ),
                ExamStagePattern(
                    stageName = "Typing Skill Test / CBAT (Psychometric)",
                    examMode = "Qualifying Test",
                    questionCount = 0,
                    maxMarks = 0,
                    durationMinutes = 10,
                    negativeMarking = "N/A",
                    qualifyingCriteria = "Required for Station Master & Junior Accounts Assistant"
                )
            ),
            cutoffTrends = listOf(
                CutoffTrend("2022", "74.80", "70.20", "67.50", "62.40", "56.80"),
                CutoffTrend("2019", "68.20", "64.10", "61.30", "58.00", "51.40"),
                CutoffTrend("2016", "77.72", "72.86", "70.10", "66.31", "59.20")
            ),
            strategyTip = "General Awareness accounts for 40 questions in CBT-1 and 50 questions in CBT-2. Deep revision of NCERT General Science up to 10th standard gives you an edge."
        ),
        "exam-nta-jee-main" to ExamPatternGuide(
            examId = "exam-nta-jee-main",
            examName = "NTA JEE Main (B.E. / B.Tech)",
            totalStages = 1,
            stages = listOf(
                ExamStagePattern(
                    stageName = "Paper 1 (Physics, Chemistry, Mathematics)",
                    examMode = "Online (Computer Based Test)",
                    questionCount = 90,
                    maxMarks = 300,
                    durationMinutes = 180,
                    negativeMarking = "-1 mark for both MCQs and Numerical Value questions",
                    qualifyingCriteria = "Top ~2,50,000 candidates qualify for JEE Advanced"
                )
            ),
            cutoffTrends = listOf(
                CutoffTrend("2024", "93.23", "79.28", "81.32", "60.09", "46.69"),
                CutoffTrend("2023", "90.77", "73.61", "75.62", "51.97", "37.23"),
                CutoffTrend("2022", "88.41", "67.00", "63.11", "43.08", "26.77")
            ),
            strategyTip = "Focus on NCERT mastery for Chemistry line-by-line; it offers the highest scoring return per minute spent in the exam."
        )
    )

    fun getGuide(examId: String): ExamPatternGuide {
        return guides[examId] ?: guides["exam-upsc-cse"]!!
    }

    fun getAllExamIds(): List<String> = guides.keys.toList()
}
