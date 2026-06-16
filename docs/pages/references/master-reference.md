---
layout: default
title: "Master Reference"
description: "PikaORM Master Reference: exhaustive technical documentation for every feature and subsystem in the framework."
active_page: master-reference
permalink: /pages/master-reference/
---

<div class="patterns-header">
    <h2 class='allcaps'>PikaORM Master Reference</h2>
    <p>The exhaustive, technical documentation for every feature and subsystem in the framework. Click any guide title for the full explanation and code examples.</p>
</div>

<section class="pattern-section" id="system-understanding">
    <h3>System Understanding</h3>
    <table class="pattern-table">
        <thead>
            <tr>
                <th>Guide</th>
                <th>Description</th>
            </tr>
        </thead>
        <tbody>
            <tr class="pattern-row">
                <td><a href="/pages/architecture/">Architecture</a></td>
                <td>A complete map of the framework's internal components.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/philosophy/">Philosophy of Pika</a></td>
                <td>The design principles guiding the ORM and the Layered API concepts.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/understanding-test-suite/">Understanding the Test Suite</a></td>
                <td>How to use the test suite as an executable reference for complex use cases.</td>
            </tr>
        </tbody>
    </table>
</section>

<section class="pattern-section" id="core-querying">
    <h3>Core Querying and Mapping</h3>
    <table class="pattern-table">
        <thead>
            <tr>
                <th>Guide</th>
                <th>Description</th>
            </tr>
        </thead>
        <tbody>
            <tr class="pattern-row">
                <td><a href="/pages/querying/">Advanced Querying</a></td>
                <td>Using <code>PikaClassQuery</code>, <code>PikaQueryBuilder</code>, and raw SQL.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/db-mapping-configuration/">Database Mapping Configuration</a></td>
                <td>Global database-to-Java mapping conventions and ORM overrides.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/custom-field-mapping/">Custom Field Mapping</a></td>
                <td>Defining class-specific mappings, serializations, and metadata.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/ejb/">EnterprisePikaBean (Active Record)</a></td>
                <td>The active record pattern, validation, and dirty-field tracking.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/coercion-system/">Coercion System</a></td>
                <td>How PikaORM translates between Java types and JDBC types seamlessly.</td>
            </tr>
        </tbody>
    </table>
</section>

<section class="pattern-section" id="relationships-performance">
    <h3>Relationships and Performance</h3>
    <table class="pattern-table">
        <thead>
            <tr>
                <th>Guide</th>
                <th>Description</th>
            </tr>
        </thead>
        <tbody>
            <tr class="pattern-row">
                <td><a href="/pages/relationships/">Relationships</a></td>
                <td>How to resolve One-to-Many, Many-to-Many, and Belongs-To relations.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/query-caching/">Query Caching API</a></td>
                <td>The thread-local query caching system for web requests.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/paging/">Paging</a></td>
                <td>Built-in pagination, total counts, and URL manipulation.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/streaming/">Streaming</a></td>
                <td>Memory-efficient JDBC data streaming and connection context management.</td>
            </tr>
        </tbody>
    </table>
</section>

<section class="pattern-section" id="infrastructure-lifecycle">
    <h3>Infrastructure and Lifecycle</h3>
    <table class="pattern-table">
        <thead>
            <tr>
                <th>Guide</th>
                <th>Description</th>
            </tr>
        </thead>
        <tbody>
            <tr class="pattern-row">
                <td><a href="/pages/transactions/">Transactions</a></td>
                <td>Managing nested transactions and connection lifecycle.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/migrations/">Migrations</a></td>
                <td>Defining and executing database schema changes entirely in Java.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/optimistic-concurrency/">Optimistic Concurrency</a></td>
                <td>Preventing lost updates and race conditions with version tracking.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/lifecycle-callbacks/">Lifecycle Callbacks</a></td>
                <td>Hooking into the database execution pipeline (beforeInsert, beforeUpdate).</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="/pages/logging/">Logging and Errors</a></td>
                <td>Configuring query logging, error handling, and thread-local suppressions.</td>
            </tr>
        </tbody>
    </table>
</section>
