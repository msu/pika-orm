---
layout: default
title: "Master Reference Hub"
description: "Central hub for navigating the core aspects of PikaORM."
active_page: master-reference
permalink: /pages/master-reference/
---

<div style="max-width: 75ch; margin: 0 auto; padding: 2rem 0;">
  <h1 id="documentation" tabindex="-1" style="text-align:center; font-size: 2.25rem; margin-bottom: 0.5rem;">Master Reference Hub</h1>
  <p class="subtitle" style="text-align:center; color: var(--ink-muted); margin-bottom: 3rem; font-size: 1.125rem;">
    The exhaustive, technical documentation for every feature and subsystem in the framework.
  </p>

  <div class="docs-grid">

    <!-- 1. System Understanding -->
    <div class="docs-card-accordion">
      <div class="docs-card-summary">
        <div class="docs-card-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
        </div>
        <strong>System Understanding</strong>
        <span>Architecture, design philosophy, and navigating the test suite</span>
      </div>
      <div class="docs-card-content">
        <ul>
          <li>
            <a href="{{ '/pages/architecture/' | relative_url }}">
              <strong>Architecture</strong>
              <small>A complete map of the framework's internal components.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/philosophy/' | relative_url }}">
              <strong>Philosophy of Pika</strong>
              <small>The design principles guiding the ORM and Layered API concepts.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/understanding-test-suite/' | relative_url }}">
              <strong>Understanding the Test Suite</strong>
              <small>How to use the test suite as an executable reference.</small>
            </a>
          </li>
        </ul>
      </div>
    </div>

    <!-- 2. Core Querying and Mapping -->
    <div class="docs-card-accordion">
      <div class="docs-card-summary">
        <div class="docs-card-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5V19A9 3 0 0 0 21 19V5"/><path d="M3 12A9 3 0 0 0 21 12"/></svg>
        </div>
        <strong>Core Querying and Mapping</strong>
        <span>Executing queries, mapping Java objects, and the coercion system</span>
      </div>
      <div class="docs-card-content">
        <ul>
          <li>
            <a href="{{ '/pages/querying/' | relative_url }}">
              <strong>Advanced Querying</strong>
              <small>Using PikaClassQuery, PikaQueryBuilder, and raw SQL.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/db-mapping-configuration/' | relative_url }}">
              <strong>Database Mapping Configuration</strong>
              <small>Global database-to-Java mapping conventions.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/custom-field-mapping/' | relative_url }}">
              <strong>Custom Field Mapping</strong>
              <small>Defining class-specific mappings and serializations.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/ejb/' | relative_url }}">
              <strong>EnterprisePikaBean (Active Record)</strong>
              <small>The active record pattern and dirty-field tracking.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/coercion-system/' | relative_url }}">
              <strong>Coercion System</strong>
              <small>How Pika translates between Java and JDBC types.</small>
            </a>
          </li>
        </ul>
      </div>
    </div>

    <!-- 3. Relationships and Performance -->
    <div class="docs-card-accordion">
      <div class="docs-card-summary">
        <div class="docs-card-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="10.49"/></svg>
        </div>
        <strong>Relationships &amp; Performance</strong>
        <span>Handling relations, N+1 avoidance, caching, and streaming</span>
      </div>
      <div class="docs-card-content">
        <ul>
          <li>
            <a href="{{ '/pages/relationships/' | relative_url }}">
              <strong>Relationships</strong>
              <small>Resolving One-to-Many, Many-to-Many, and Belongs-To.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/query-caching/' | relative_url }}">
              <strong>Query Caching API</strong>
              <small>Thread-local query caching system for web requests.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/paging/' | relative_url }}">
              <strong>Paging</strong>
              <small>Built-in pagination, total counts, and URL manipulation.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/streaming/' | relative_url }}">
              <strong>Streaming</strong>
              <small>Memory-efficient JDBC data streaming and contexts.</small>
            </a>
          </li>
        </ul>
      </div>
    </div>

    <!-- 4. Infrastructure and Lifecycle -->
    <div class="docs-card-accordion">
      <div class="docs-card-summary">
        <div class="docs-card-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v20"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
        </div>
        <strong>Infrastructure &amp; Lifecycle</strong>
        <span>Transactions, schema migrations, and database event hooks</span>
      </div>
      <div class="docs-card-content">
        <ul>
          <li>
            <a href="{{ '/pages/transactions/' | relative_url }}">
              <strong>Transactions</strong>
              <small>Managing nested transactions and connection lifecycle.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/migrations/' | relative_url }}">
              <strong>Migrations</strong>
              <small>Executing database schema changes entirely in Java.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/optimistic-concurrency/' | relative_url }}">
              <strong>Optimistic Concurrency</strong>
              <small>Preventing lost updates with version tracking.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/lifecycle-callbacks/' | relative_url }}">
              <strong>Lifecycle Callbacks</strong>
              <small>Hooking into the database execution pipeline.</small>
            </a>
          </li>
          <li>
            <a href="{{ '/pages/logging/' | relative_url }}">
              <strong>Logging and Errors</strong>
              <small>Configuring query logging and thread-local suppressions.</small>
            </a>
          </li>
        </ul>
      </div>

  </div>
</div>
