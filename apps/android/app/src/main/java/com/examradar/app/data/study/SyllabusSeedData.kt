package com.examradar.app.data.study

import com.examradar.app.data.local.SyllabusTopicEntity

/**
 * Authentic syllabus blueprints mapped directly from official exam circulars
 * (UPSC CSE, SSC CGL, IBPS PO, RRB NTPC, JEE Main, NEET UG).
 */
object SyllabusSeedData {

    fun getAllSeedTopics(): List<SyllabusTopicEntity> {
        val topics = mutableListOf<SyllabusTopicEntity>()
        var order = 1

        fun addTopic(examId: String, stage: String, subject: String, title: String) {
            val id = "topic_${examId}_${stage.lowercase().replace(" ", "_")}_${order++}"
            topics.add(
                SyllabusTopicEntity(
                    id = id,
                    examId = examId,
                    stage = stage,
                    subject = subject,
                    title = title,
                    isCompleted = false,
                    sortOrder = order
                )
            )
        }

        // ==========================================
        // 1. UPSC Civil Services Examination (CSE)
        // ==========================================
        val upsc = "exam-upsc-cse"

        // Prelims Paper 1: General Studies
        addTopic(upsc, "Prelims", "Indian Polity & Governance", "Constitution, Preamble & Salient Features")
        addTopic(upsc, "Prelims", "Indian Polity & Governance", "Fundamental Rights, DPSP & Fundamental Duties")
        addTopic(upsc, "Prelims", "Indian Polity & Governance", "Parliament, Union Executive & Judiciary")
        addTopic(upsc, "Prelims", "Indian Polity & Governance", "Federal Structure, Center-State Relations & Emergency Provisions")
        addTopic(upsc, "Prelims", "Indian Polity & Governance", "Panchayati Raj, Municipalities & Local Governance")
        addTopic(upsc, "Prelims", "Indian Polity & Governance", "Constitutional & Non-Constitutional Statutory Bodies")

        addTopic(upsc, "Prelims", "History of India & National Movement", "Ancient India: Indus Valley, Vedic Age & Buddhism/Jainism")
        addTopic(upsc, "Prelims", "History of India & National Movement", "Medieval India: Delhi Sultanate, Mughals & Bhakti Movement")
        addTopic(upsc, "Prelims", "History of India & National Movement", "British Rule, 1857 Revolt & Freedom Movement 1885-1947")
        addTopic(upsc, "Prelims", "History of India & National Movement", "Indian Art, Architecture, Literature & Festivals")

        addTopic(upsc, "Prelims", "Indian & World Geography", "Physical Geography: Geomorphology, Climatology & Oceanography")
        addTopic(upsc, "Prelims", "Indian & World Geography", "Indian Geography: River Systems, Monsoons, Soils & Agriculture")
        addTopic(upsc, "Prelims", "Indian & World Geography", "Economic & Human Geography: Resources, Industries & Demographics")

        addTopic(upsc, "Prelims", "Economic & Social Development", "National Income, Inflation & Monetary Policy")
        addTopic(upsc, "Prelims", "Economic & Social Development", "Fiscal Policy, Union Budget & Taxation System")
        addTopic(upsc, "Prelims", "Economic & Social Development", "Banking System, RBI, Financial Inclusion & Poverty Alleviation")
        addTopic(upsc, "Prelims", "Economic & Social Development", "External Sector: Balance of Payments, WTO & Forex")

        addTopic(upsc, "Prelims", "Environment, Ecology & Climate Change", "Ecosystems, Food Chains & Biomes")
        addTopic(upsc, "Prelims", "Environment, Ecology & Climate Change", "Biodiversity Hotspots, Protected Areas & Red List Species")
        addTopic(upsc, "Prelims", "Environment, Ecology & Climate Change", "Climate Change, COP Summits, Montreal & Kyoto Protocols")
        addTopic(upsc, "Prelims", "Environment, Ecology & Climate Change", "Environmental Pollution, Solid Waste & Renewable Energy")

        addTopic(upsc, "Prelims", "General Science & Tech", "Space Missions (ISRO), Biotechnology & Genetics")
        addTopic(upsc, "Prelims", "General Science & Tech", "Artificial Intelligence, Quantum Tech & Semiconductors")
        addTopic(upsc, "Prelims", "General Science & Tech", "Defense Tech: Missiles, Radars & Submarines")

        // Prelims Paper 2: CSAT
        addTopic(upsc, "Prelims", "CSAT (Aptitude)", "Reading Comprehension & Critical Inference")
        addTopic(upsc, "Prelims", "CSAT (Aptitude)", "Logical Reasoning, Syllogisms & Seating Arrangements")
        addTopic(upsc, "Prelims", "CSAT (Aptitude)", "Basic Numeracy: Number Systems, Divisibility & Remainders")
        addTopic(upsc, "Prelims", "CSAT (Aptitude)", "Percentage, Ratio & Proportion, Profit & Loss")
        addTopic(upsc, "Prelims", "CSAT (Aptitude)", "Time, Speed, Distance & Work Equations")
        addTopic(upsc, "Prelims", "CSAT (Aptitude)", "Data Interpretation: Tables, Bar Graphs & Pie Charts")

        // Mains Overview
        addTopic(upsc, "Mains", "GS Paper 1 (Culture, History & Society)", "Modern Indian History from middle of 18th Century")
        addTopic(upsc, "Mains", "GS Paper 1 (Culture, History & Society)", "Salient features of Indian Society & Diversity")
        addTopic(upsc, "Mains", "GS Paper 1 (Culture, History & Society)", "Geophysical Phenomena & Distribution of Natural Resources")
        addTopic(upsc, "Mains", "GS Paper 2 (Governance & International Relations)", "Constitution, Executive, Judiciary & Citizen Charters")
        addTopic(upsc, "Mains", "GS Paper 2 (Governance & International Relations)", "Welfare Schemes for Vulnerable Sections & Social Justice")
        addTopic(upsc, "Mains", "GS Paper 2 (Governance & International Relations)", "India and its Neighborhood Relations & Global Groupings")
        addTopic(upsc, "Mains", "GS Paper 3 (Economy, Tech & Security)", "Indian Economy, Resource Mobilization & Inclusive Growth")
        addTopic(upsc, "Mains", "GS Paper 3 (Economy, Tech & Security)", "Internal Security Challenges & Disaster Management")
        addTopic(upsc, "Mains", "GS Paper 4 (Ethics, Integrity & Aptitude)", "Ethics and Human Interface & Foundational Values")
        addTopic(upsc, "Mains", "GS Paper 4 (Ethics, Integrity & Aptitude)", "Emotional Intelligence & Case Studies on Administrative Dilemmas")

        // ==========================================
        // 2. SSC Combined Graduate Level (CGL)
        // ==========================================
        val ssc = "exam-ssc-cgl"

        addTopic(ssc, "Tier 1", "General Intelligence & Reasoning", "Analogy, Classification & Series (Number, Alphabet)")
        addTopic(ssc, "Tier 1", "General Intelligence & Reasoning", "Coding-Decoding & Blood Relations")
        addTopic(ssc, "Tier 1", "General Intelligence & Reasoning", "Venn Diagrams & Syllogisms")
        addTopic(ssc, "Tier 1", "General Intelligence & Reasoning", "Non-Verbal: Paper Folding, Mirror Images & Matrix")

        addTopic(ssc, "Tier 1", "Quantitative Aptitude", "Number Systems, Computation of Whole Numbers & Decimals")
        addTopic(ssc, "Tier 1", "Quantitative Aptitude", "Percentage, Ratio & Proportion, Square Roots, Averages")
        addTopic(ssc, "Tier 1", "Quantitative Aptitude", "Profit & Loss, Discount, Partnership Business & Mixture")
        addTopic(ssc, "Tier 1", "Quantitative Aptitude", "Time and Distance, Time & Work")
        addTopic(ssc, "Tier 1", "Quantitative Aptitude", "Basic Algebraic Identities & Elementary Surds")
        addTopic(ssc, "Tier 1", "Quantitative Aptitude", "Triangle & its Centers, Circles, Chords & Tangents")
        addTopic(ssc, "Tier 1", "Quantitative Aptitude", "Trigonometric Ratios, Degree & Radian Measures, Heights & Distances")
        addTopic(ssc, "Tier 1", "Quantitative Aptitude", "Histograms, Frequency Polygon & Bar-diagram/Pie-chart")

        addTopic(ssc, "Tier 1", "English Comprehension", "Spotting Errors, Fill in the Blanks & Sentence Improvement")
        addTopic(ssc, "Tier 1", "English Comprehension", "Synonyms, Antonyms, Idioms & Phrases, One-Word Substitution")
        addTopic(ssc, "Tier 1", "English Comprehension", "Active/Passive Voice & Direct/Indirect Speech")
        addTopic(ssc, "Tier 1", "English Comprehension", "Cloze Test & Reading Comprehension Passages")

        addTopic(ssc, "Tier 1", "General Awareness", "Indian History & Freedom Movement")
        addTopic(ssc, "Tier 1", "General Awareness", "Geography (Physical & Economic) & Indian Polity")
        addTopic(ssc, "Tier 1", "General Awareness", "General Science (Physics, Chemistry, Biology) & Scientific Discoveries")
        addTopic(ssc, "Tier 1", "General Awareness", "Current National & International Events & Sports")

        // ==========================================
        // 3. IBPS Probationary Officer (CRP PO)
        // ==========================================
        val ibps = "exam-ibps-po"

        addTopic(ibps, "Prelims", "Quantitative Aptitude", "Simplification, Approximation & Number Series")
        addTopic(ibps, "Prelims", "Quantitative Aptitude", "Quadratic Equations & Polynomial Inequalities")
        addTopic(ibps, "Prelims", "Quantitative Aptitude", "Data Interpretation (Tabular, Line, Bar, Radar & Caselet)")
        addTopic(ibps, "Prelims", "Quantitative Aptitude", "Arithmetic: Percentage, Profit-Loss, Simple & Compound Interest")
        addTopic(ibps, "Prelims", "Quantitative Aptitude", "Time-Speed-Distance, Trains, Boats & Streams, Pipes & Cisterns")

        addTopic(ibps, "Prelims", "Reasoning Ability", "Seating Arrangement (Circular, Linear & Parallel Lines)")
        addTopic(ibps, "Prelims", "Reasoning Ability", "Puzzles (Floor, Box, Month-Day & Category)")
        addTopic(ibps, "Prelims", "Reasoning Ability", "Syllogisms (Only a few, Either-Or, Possibility)")
        addTopic(ibps, "Prelims", "Reasoning Ability", "Inequalities & Coding-Decoding")
        addTopic(ibps, "Prelims", "Reasoning Ability", "Blood Relations, Direction Sense & Order-Ranking")

        addTopic(ibps, "Prelims", "English Language", "Reading Comprehension with Vocab Inferences")
        addTopic(ibps, "Prelims", "English Language", "Error Detection, Phrase Replacement & Column Matching")
        addTopic(ibps, "Prelims", "English Language", "Cloze Test, Para Jumbles & Sentence Rearrangement")

        // ==========================================
        // 4. Railway Recruitment Board (RRB NTPC)
        // ==========================================
        val rrb = "exam-rrb-ntpc"

        addTopic(rrb, "CBT 1", "Mathematics", "Number System, Decimals, Fractions, LCM & HCF")
        addTopic(rrb, "CBT 1", "Mathematics", "Ratio and Proportions, Percentages, Mensuration")
        addTopic(rrb, "CBT 1", "Mathematics", "Time and Work, Time and Distance, Simple and Compound Interest")
        addTopic(rrb, "CBT 1", "Mathematics", "Profit and Loss, Elementary Algebra, Geometry and Trigonometry")

        addTopic(rrb, "CBT 1", "General Intelligence & Reasoning", "Analogies, Completion of Number and Alphabetical Series")
        addTopic(rrb, "CBT 1", "General Intelligence & Reasoning", "Coding and Decoding, Mathematical Operations, Similarities")
        addTopic(rrb, "CBT 1", "General Intelligence & Reasoning", "Jumbling, Venn Diagrams, Puzzle, Data Sufficiency")
        addTopic(rrb, "CBT 1", "General Intelligence & Reasoning", "Statement-Conclusion, Statement-Courses of Action")

        addTopic(rrb, "CBT 1", "General Awareness", "Current Events of National and International Importance")
        addTopic(rrb, "CBT 1", "General Awareness", "Games and Sports, Art and Culture of India, Indian Literature")
        addTopic(rrb, "CBT 1", "General Awareness", "Monuments and Places of India, General Science and Life Science (up to 10th CBSE)")
        addTopic(rrb, "CBT 1", "General Awareness", "History of India and Freedom Struggle, Physical, Social and Economic Geography")
        addTopic(rrb, "CBT 1", "General Awareness", "Indian Polity and Governance- constitution and political system")

        // ==========================================
        // 5. NTA JEE Main
        // ==========================================
        val jee = "exam-nta-jee-main"

        addTopic(jee, "Session 1 & 2", "Physics", "Units & Measurements, Kinematics & Laws of Motion")
        addTopic(jee, "Session 1 & 2", "Physics", "Work, Energy & Power, Rotational Motion & Gravitation")
        addTopic(jee, "Session 1 & 2", "Physics", "Thermodynamics, Kinetic Theory of Gases & Oscillations")
        addTopic(jee, "Session 1 & 2", "Physics", "Electrostatics, Current Electricity & Magnetic Effects")
        addTopic(jee, "Session 1 & 2", "Physics", "Electromagnetic Induction, Alternating Currents & Optics")
        addTopic(jee, "Session 1 & 2", "Physics", "Dual Nature of Matter, Atoms, Nuclei & Semiconductor Devices")

        addTopic(jee, "Session 1 & 2", "Chemistry", "Some Basic Concepts of Chemistry & Atomic Structure")
        addTopic(jee, "Session 1 & 2", "Chemistry", "Chemical Bonding and Molecular Structure & Thermodynamics")
        addTopic(jee, "Session 1 & 2", "Chemistry", "Equilibrium, Redox Reactions & Chemical Kinetics")
        addTopic(jee, "Session 1 & 2", "Chemistry", "Coordination Compounds, d- and f-Block Elements")
        addTopic(jee, "Session 1 & 2", "Chemistry", "Hydrocarbons, Organic Compounds with Functional Groups")

        addTopic(jee, "Session 1 & 2", "Mathematics", "Sets, Relations and Functions, Complex Numbers & Quadratic Equations")
        addTopic(jee, "Session 1 & 2", "Mathematics", "Matrices and Determinants, Permutations and Combinations")
        addTopic(jee, "Session 1 & 2", "Mathematics", "Sequences and Series, Limit, Continuity and Differentiability")
        addTopic(jee, "Session 1 & 2", "Mathematics", "Integral Calculus & Differential Equations")
        addTopic(jee, "Session 1 & 2", "Mathematics", "Coordinate Geometry, 3D Geometry & Vector Algebra")
        addTopic(jee, "Session 1 & 2", "Mathematics", "Probability & Trigonometry")

        return topics
    }
}
