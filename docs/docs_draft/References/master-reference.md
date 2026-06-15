---
title: "Master Reference"
layout: default
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
                <td><a href="{{ site.baseurl }}/docs_draft/References/System%20Understanding/Architecture.html">Architecture</a></td>
                <td>A complete map of the framework's internal components.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/System%20Understanding/philosophy-of-pika.html">Philosophy of Pika</a></td>
                <td>The design principles guiding the ORM and the Layered API concepts.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/System%20Understanding/understanding-the-test-suite.html">Understanding the Test Suite</a></td>
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
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/advanced-querying.html">Advanced Querying</a></td>
                <td>Using <code>PikaClassQuery</code>, <code>PikaQueryBuilder</code>, and raw SQL.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/db-mapping-configuration.html">Database Mapping Configuration</a></td>
                <td>Global database-to-Java mapping conventions and ORM overrides.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/custom-field-mapping.html">Custom Field Mapping</a></td>
                <td>Defining class-specific mappings, serializations, and metadata.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/enterprise-java-beans-support.html">EnterprisePikaBean (Active Record)</a></td>
                <td>The active record pattern, validation, and dirty-field tracking.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/coercion-system.html">Coercion System</a></td>
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
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/relationships.html">Relationships</a></td>
                <td>How to resolve One-to-Many, Many-to-Many, and Belongs-To relations.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/query-caching.html">Query Caching API</a></td>
                <td>The thread-local query caching system for web requests.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/paging.html">Paging</a></td>
                <td>Built-in pagination, total counts, and URL manipulation.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/streaming.html">Streaming</a></td>
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
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/transactions.html">Transactions</a></td>
                <td>Managing nested transactions and connection lifecycle.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/migrations.html">Migrations</a></td>
                <td>Defining and executing database schema changes entirely in Java.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/optimistic-concurrency.html">Optimistic Concurrency</a></td>
                <td>Preventing lost updates and race conditions with version tracking.</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/lifecycle-callbacks.html">Lifecycle Callbacks</a></td>
                <td>Hooking into the database execution pipeline (beforeInsert, beforeUpdate).</td>
            </tr>
            <tr class="pattern-row">
                <td><a href="{{ site.baseurl }}/docs_draft/References/Technical%20Feature%20Guides/logging-errors.html">Logging and Errors</a></td>
                <td>Configuring query logging, error handling, and thread-local suppressions.</td>
            </tr>
        </tbody>
    </table>
</section>
