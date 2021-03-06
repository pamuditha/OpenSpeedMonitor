import org.apache.commons.logging.LogFactory

databaseChangeLog = {
    // ### BEGIN Refactoring of tag attribute ###
    // split eventResultTag
    changeSet(author: "mmi", id: "1472807664005-1") {
        preConditions(onFail: 'MARK_RAN') {
            not {
                sqlCheck(expectedResult: '0', 'select count(*) from event_result')
            }
        }
        grailsChange {
            change {
                int minID = sql.firstRow("SELECT MIN(ID) FROM event_result").getAt(0)
                int maxID = sql.firstRow("SELECT MAX(ID) FROM event_result").getAt(0)
                int batchSize = 10000
                def log = LogFactory.getLog("liquibase")

                log.debug("split eventResultTag: Migrating event_result table (${maxID - minID} rows, batchSize ${batchSize}")

                minID.step(maxID, batchSize) {
                    sql.executeUpdate('''
                        UPDATE event_result SET
                            job_group_id = (SUBSTRING_INDEX(tag, ';', 1)),
                            measured_event_id = SUBSTRING_INDEX(SUBSTRING_INDEX(tag, ";", -4), ";", 1),
                            page_id = SUBSTRING_INDEX(SUBSTRING_INDEX(tag, ";", -3), ";", 1),
                            browser_id = SUBSTRING_INDEX(SUBSTRING_INDEX(tag, ";", -2), ";", 1),
                            location_id = SUBSTRING_INDEX(SUBSTRING_INDEX(tag, ";", -1), ";", 1)
                        WHERE id between :lowerLimit AND :upperLimit
                                        ''', ["lowerLimit": it, "upperLimit": it + batchSize])
                    float percent = ((float) (it+batchSize - minID)) / (maxID - minID) * 100
                    log.debug("Migrated ${percent} percent (${(it+batchSize - minID)}/${maxID - minID} rows)... ")
                }

                log.debug("split eventResultTag migration finished")

            }
        }
    }

    // split measuredEventTag
    changeSet(author: "mmi", id: "1472807664001-1") {
        preConditions(onFail: 'MARK_RAN') {
            not {
                sqlCheck(expectedResult: '0', 'select count(*) from csi_aggregation')
            }
        }
        grailsChange {
            change {
                int minID = sql.firstRow("SELECT MIN(ID) FROM csi_aggregation").getAt(0)
                int maxID = sql.firstRow("SELECT MAX(ID) FROM csi_aggregation").getAt(0)
                int batchSize = 10000
                def log = LogFactory.getLog("liquibase")

                log.debug("split measuredEventTag: Migrating csi_aggregation table (${maxID - minID} rows, batchSize ${batchSize}")

                minID.step(maxID, batchSize) {
                    sql.executeUpdate('''
                        UPDATE csi_aggregation SET
                            job_group_id = (SUBSTRING_INDEX(tag, ';', 1)),
                            measured_event_id = SUBSTRING_INDEX(SUBSTRING_INDEX(tag, ";", -4), ";", 1),
                            page_id = SUBSTRING_INDEX(SUBSTRING_INDEX(tag, ";", -3), ";", 1),
                            browser_id = SUBSTRING_INDEX(SUBSTRING_INDEX(tag, ";", -2), ";", 1),
                            location_id = SUBSTRING_INDEX(SUBSTRING_INDEX(tag, ";", -1), ";", 1)
                        WHERE (id between :lowerLimit AND :upperLimit) AND aggregator_id = (select id from aggregator_type where name = 'measuredEvent')
                                        ''', ["lowerLimit": it, "upperLimit": it + batchSize])
                    float percent = ((float) (it+batchSize - minID)) / (maxID - minID) * 100
                    log.debug("Migrated ${percent} percent (${(it+batchSize - minID)}/${maxID - minID} rows)... ")
                }

                log.debug("split measuredEventTag migration finished")
            }
        }
    }

    // split pageTag
    changeSet(author: "mmi", id: "1472807664001-2") {
        preConditions(onFail: 'MARK_RAN') {
            not {
                sqlCheck(expectedResult: '0', 'select count(*) from csi_aggregation')
            }
        }
        grailsChange {
            change {
                int minID = sql.firstRow("SELECT MIN(ID) FROM csi_aggregation").getAt(0)
                int maxID = sql.firstRow("SELECT MAX(ID) FROM csi_aggregation").getAt(0)
                int batchSize = 10000
                def log = LogFactory.getLog("liquibase")

                log.debug("split pageTag: Migrating csi_aggregation table (${maxID - minID} rows, batchSize ${batchSize}")

                minID.step(maxID, batchSize) {
                    sql.executeUpdate('''
                        UPDATE csi_aggregation SET
                            job_group_id = (SUBSTRING_INDEX(tag, ';', 1)),
                            page_id = SUBSTRING_INDEX(tag, ";", -1)
                        WHERE (id between :lowerLimit AND :upperLimit) AND aggregator_id = (select id from aggregator_type where name = 'page')
                                        ''', ["lowerLimit": it, "upperLimit": it + batchSize])
                    float percent = ((float) (it+batchSize - minID)) / (maxID - minID) * 100
                    log.debug("Migrated ${percent} percent (${(it+batchSize - minID)}/${maxID - minID} rows)... ")
                }

                log.debug("split pageTag migration finished")
            }
        }
    }

    // split shopTag
    changeSet(author: "mmi", id: "1472807664001-3") {
        preConditions(onFail: 'MARK_RAN') {
            not {
                sqlCheck(expectedResult: '0', 'select count(*) from csi_aggregation')
            }
        }
        grailsChange {
            change {
                int minID = sql.firstRow("SELECT MIN(ID) FROM csi_aggregation").getAt(0)
                int maxID = sql.firstRow("SELECT MAX(ID) FROM csi_aggregation").getAt(0)
                int batchSize = 10000
                def log = LogFactory.getLog("liquibase")

                log.debug("split shopTag: Migrating csi_aggregation table (${maxID - minID} rows, batchSize ${batchSize}")

                minID.step(maxID, batchSize) {
                    sql.executeUpdate('''
                        UPDATE csi_aggregation SET
                            job_group_id = tag
                        WHERE (id between :lowerLimit AND :upperLimit) AND aggregator_id = (select id from aggregator_type where name = 'shop')
                                        ''', ["lowerLimit": it, "upperLimit": it + batchSize])
                    float percent = ((float) (it+batchSize - minID)) / (maxID - minID) * 100
                    log.debug("Migrated ${percent} percent (${(it+batchSize - minID)}/${maxID - minID} rows)... ")
                }

                log.debug("split shopTag migration finished")
            }
        }
    }




    changeSet(author: "mmi", id: "1473397000359-13") {
        dropColumn(columnName: "tag", tableName: "event_result")
    }
    changeSet(author: "mmi", id: "1473397000359-14") {
        dropColumn(columnName: "tag", tableName: "csi_aggregation")
    }

    // cleanup foreign key constraints
    changeSet(author: "mmi", id: "1472807664001-15") {
        preConditions(onFail: 'MARK_RAN') {
            not {
                sqlCheck(expectedResult: '0', 'select count(*) from csi_aggregation')
            }
        }
        grailsChange {
            change {
                sql.executeUpdate('''
                        UPDATE csi_aggregation SET
                            job_group_id = null 
                        WHERE job_group_id IS NOT NULL AND job_group_id NOT IN (SELECT id FROM job_group)''')
                sql.executeUpdate('''
                        UPDATE csi_aggregation SET
                            measured_event_id = null 
                        WHERE measured_event_id IS NOT null AND measured_event_id NOT IN (SELECT id FROM measured_event)''')
                sql.executeUpdate('''
                        UPDATE csi_aggregation SET
                            page_id = null 
                        WHERE page_id IS NOT null AND page_id NOT IN (SELECT id FROM page)''')
                sql.executeUpdate('''
                        UPDATE csi_aggregation SET
                            browser_id = null 
                        WHERE browser_id IS NOT null AND browser_id NOT IN (SELECT id FROM browser)''')
                sql.executeUpdate('''
                        UPDATE csi_aggregation SET
                            location_id = null 
                        WHERE location_id IS NOT null AND location_id NOT IN (SELECT id FROM location)''')
            }

        }
    }
    changeSet(author: "mmi", id: "1472807664001-16") {
        preConditions(onFail: 'MARK_RAN') {
            not {
                sqlCheck(expectedResult: '0', 'select count(*) from event_result')
            }
        }
        grailsChange {
            change {
                sql.executeUpdate('''
                        UPDATE event_result SET
                            job_group_id = null 
                        WHERE job_group_id IS NOT NULL AND job_group_id NOT IN (SELECT id FROM job_group)''')
                sql.executeUpdate('''
                        UPDATE event_result SET
                            measured_event_id = null 
                        WHERE measured_event_id IS NOT null AND measured_event_id NOT IN (SELECT id FROM measured_event)''')
                sql.executeUpdate('''
                        UPDATE event_result SET
                            page_id = null 
                        WHERE page_id IS NOT null AND page_id NOT IN (SELECT id FROM page)''')
                sql.executeUpdate('''
                        UPDATE event_result SET
                            browser_id = null 
                        WHERE browser_id IS NOT null AND browser_id NOT IN (SELECT id FROM browser)''')
                sql.executeUpdate('''
                        UPDATE event_result SET
                            location_id = null 
                        WHERE location_id IS NOT null AND location_id NOT IN (SELECT id FROM location)''')
            }

        }
    }


    changeSet(author: "mmi", id: "1473397000359-17") {
        addForeignKeyConstraint(baseColumnNames: "page_id", baseTableName: "csi_aggregation", constraintName: "FK_ed4k8n53a9o4f2eyjf1wwn5h0", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "page")
        addForeignKeyConstraint(baseColumnNames: "measured_event_id", baseTableName: "csi_aggregation", constraintName: "FK_m2ihng827m0wdm7lhjw8wd0k8", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "measured_event")
        addForeignKeyConstraint(baseColumnNames: "job_group_id", baseTableName: "csi_aggregation", constraintName: "FK_pep8a7052p26u3gixxny6wmp7", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "job_group")
        addForeignKeyConstraint(baseColumnNames: "browser_id", baseTableName: "csi_aggregation", constraintName: "FK_pj2t91buh552uxodjasjpxxjh", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "browser")
        addForeignKeyConstraint(baseColumnNames: "location_id", baseTableName: "csi_aggregation", constraintName: "FK_rtpndsvi65apepp2ej6lkdns5", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "location")

        addForeignKeyConstraint(baseColumnNames: "location_id", baseTableName: "event_result", constraintName: "FK_1lqpe6ckkhtlm17wdcqpp0bf4", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "location")
        addForeignKeyConstraint(baseColumnNames: "browser_id", baseTableName: "event_result", constraintName: "FK_3a0bt55f0gmtigcetbyg2bovk", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "browser")
        addForeignKeyConstraint(baseColumnNames: "page_id", baseTableName: "event_result", constraintName: "FK_5sk7tn8qu8oyb6h4kxs555lvd", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "page")
        addForeignKeyConstraint(baseColumnNames: "job_group_id", baseTableName: "event_result", constraintName: "FK_nd8wwj4cql52rwjpo11njys5r", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "job_group")
        addForeignKeyConstraint(baseColumnNames: "measured_event_id", baseTableName: "event_result", constraintName: "FK_3CA811628B5E24AF", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "measured_event")
    }
    // ### END Refactoring of tag attribute ###

    // Delete csiAggregationUpdateEvents which belong to already deleted csiAggregations.
    changeSet(author: "mmi", id: "1477505809000-1") {
        sql('''
            delete from csi_aggregation_update_event where csi_aggregation_id not in (
                select id from csi_aggregation);
        ''')
    }

    // ### BEGIN job statistic ###

    changeSet(author: "nkuhn", id: "1479457659-1") {
        createTable(tableName: "job_statistic") {
            column(autoIncrement: "true", name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "job_statistic_id_tyPK")
            }

            column(name: "version", type: "bigint") {
                constraints(nullable: "false")
            }

            column(name: "percentage_successful_tests_of_last5", type: "double precision") {
                constraints(nullable: "true")
            }
            column(name: "percentage_successful_tests_of_last25", type: "double precision") {
                constraints(nullable: "true")
            }
            column(name: "percentage_successful_tests_of_last150", type: "double precision") {
                constraints(nullable: "true")
            }

        }
    }
    changeSet(author: "nkuhn", id: "1479457659-2") {
        addColumn(tableName: "job") {
            column(name: "job_statistic_id", type: "bigint")
        }
    }
    changeSet(author: "nkuhn", id: "1479457659-3") {
        addForeignKeyConstraint(baseColumnNames: "job_statistic_id", baseTableName: "job", constraintName: "FK_job_job_statistic_id", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "job_statistic")
    }

    changeSet(author: "nkuhn", id: "1480788858-1") {
        grailsChange {
            change {

                def log = LogFactory.getLog("liquibase")
                float successfulWithinLast150
                float successfulWithinLast25
                float successfulWithinLast5
                float percentageLast150
                float percentageLast25
                float percentageLast5

                sql.eachRow("select id,label from job;") { row ->

                    log.debug("calculation of job stats for job '${row.label}'")

                    successfulWithinLast150 = sql.firstRow("select count(subselect_table.id) from \n" +
                            "(select id,http_status_code from job_result where job_id = ${row.id} and http_status_code >= 200 order by date desc limit 150) subselect_table \n" +
                            "where subselect_table.http_status_code = 200;")[0]
                    successfulWithinLast25 = sql.firstRow("select count(subselect_table.id) from \n" +
                            "(select id,http_status_code from job_result where job_id = ${row.id} and http_status_code >= 200 order by date desc limit 25) subselect_table \n" +
                            "where subselect_table.http_status_code = 200;")[0]
                    successfulWithinLast5 = sql.firstRow("select count(subselect_table.id) from \n" +
                            "(select id,http_status_code from job_result where job_id = ${row.id} and http_status_code >= 200 order by date desc limit 5) subselect_table \n" +
                            "where subselect_table.http_status_code = 200;")[0]
                    percentageLast150 = (successfulWithinLast150/150)*100
                    percentageLast25 = (successfulWithinLast25/25)*100
                    percentageLast5 = (successfulWithinLast5/5)*100
                    log.debug "percentageLast5 = $percentageLast5"
                    log.debug "percentageLast25 = $percentageLast25"
                    log.debug "percentageLast150 = $percentageLast150"

                    log.debug("insert job_statistic for job '${row.label}'")
                    long new_job_stat_id = sql.executeInsert("insert into job_statistic (version, percentage_successful_tests_of_last5,percentage_successful_tests_of_last25,percentage_successful_tests_of_last150) " +
                            "values (0, ${percentageLast5}, ${percentageLast25}, ${percentageLast150})")[0][0]
                    log.debug("insert job_statistic for job '${row.label}'... DONE")
                    log.debug("set id of new job_stat for job '${row.label}'")
                    sql.executeUpdate("update job set job_statistic_id = ${new_job_stat_id} where id = ${row.id}")
                    log.debug("set id of new job_stat for job '${row.label}'... DONE")
                }
            }
        }
    }

    // ### END job statistic ###

}