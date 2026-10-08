| Mutant | Change | Verdict | Killed by |
| --- | --- | --- | --- |
| M01 | chessboard distance loses its north-west diagonal | KILLED | GameTest macaws_roof_blocks_work_as_stairs, RoofPlannerTest.[2] HIP, RoofPlannerTest.[5] MANSARD, RoofPlannerTest.[6] PYRAMID (+3 more) |
| M02 | stairs face downhill | KILLED | RoofPlannerTest.evenWidthGableMeetsBackToBackWithoutARidgeCap(), RoofPlannerTest.gableRidgeFollowsTheLongSideUnlessAnAxisIsForced(), RoofPlannerTest.hipOnRectangleRisesOneBlockPerRingWithOuterCornersOnTheHips(), RoofPlannerTest.shedRisesTowardsTheChosenSide() |
| M03 | outer corners mirrored | KILLED | GameTest macaws_roof_blocks_work_as_stairs, GameTest placed_stairs_keep_their_shapes, StairShapesTest.outerCornerIsRefusedWhenTheSideContinuesTheRun(), StairShapesTest.outerWinsOverInner() (+1 more) |
| M04 | columns no longer fill down to their lowest neighbour | KILLED | RoofPlannerTest.[3] DUTCH_GABLE, RoofPlannerTest.[7] SHED, RoofPlannerTest.[8] CONE |
| M05 | gable walls one block short | KILLED | RoofPlannerTest.gableHasStraightRakesAndATriangularGableWall() |
| M06 | attached pieces never become arms | KILLED | RoofPlannerTest.aBumpGetsASmallCrossGableThatStopsAtTheMainRidge(), RoofPlannerTest.armsNeverRiseAboveTheWingTheyJoin(), RoofPlannerTest.ellArmEndsInAValleyInsteadOfASecondGableAtTheCorner() |
| M07 | force replaces block entities | KILLED | GameTest force_replaces_only_plain_blocks |
| M08 | replaceable blocks and fluids overwritten without consent | KILLED | GameTest air_only_by_default |
| M09 | undo and redo overwrite blocks changed since | KILLED | GameTest undo_restores_and_skips_edited_blocks |
| M10 | per-tick block budget ignored | KILLED | GameTest placement_spreads_across_ticks |
| M11 | claims ignored | KILLED | GameTest claimed_blocks_are_skipped |
| M12 | a new roof keeps the redo stack | KILLED | GameTest redo_reapplies_and_new_roof_clears_redo, HistoryTest.aNewEntryClearsRedo() |
| M13 | walls joined only side by side | KILLED | FootprintDetectorTest.wallsTouchingOnlyAtCornersStillClose() |
| M14 | everyone may use /roof | KILLED | GameTest commands_need_permission |
| M15 | preview cap ignored | KILLED | GameTest preview_is_packet_only_capped_and_marks_blocked |
| M16 | a zero block budget is accepted | KILLED | RoofwrightConfigTest.outOfRangeValuesAreClampedWithAWarning(Path) |
| M17 | low pitch rises a full block | KILLED | RoofPlannerTest.lowPitchIsAllSlabsAndRisesHalfABlockPerColumn() |
| M18 | families lose their wall block | KILLED | GameTest materials_resolve_from_any_family_member |
| M19 | shed rises the wrong way | KILLED | RoofPlannerTest.shedRisesTowardsTheChosenSide() |
| M20 | ridges never capped | KILLED | GameTest adventure_mode_cannot_build, GameTest air_only_by_default, GameTest claimed_blocks_are_skipped, GameTest force_replaces_only_plain_blocks (+28 more) |
