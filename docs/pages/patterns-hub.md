---
layout: default
title: "Patterns & Examples Hub"
description: "Design patterns, examples, and comprehensive guides for using PikaORM effectively."
active_page: patterns-hub
permalink: /pages/patterns-hub/
---

<div style="max-width: 75ch; margin: 0 auto; padding: 2rem 0;">
  <h1 id="patterns" tabindex="-1" style="text-align:center; font-size: 2.25rem; margin-bottom: 0.5rem;">Patterns &amp; Examples</h1>
  <p class="subtitle" style="text-align:center; color: var(--ink-muted); margin-bottom: 3rem; font-size: 1.125rem;">
    Real-world examples and common design patterns using PikaORM.
  </p>

  <h2 style="margin-bottom: 1.5rem; font-size: 1.5rem; border-bottom: 1px solid var(--surface-3); padding-bottom: 0.5rem;">Feature Patterns</h2>
  <div class="docs-grid" style="margin-bottom: 3rem;">

    <!-- 1. Connection to Standard Logging -->
    <div class="docs-card-accordion">
      <div class="docs-card-summary">
        <div class="docs-card-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v20"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
        </div>
        <strong>Standard Logging Interfaces</strong>
        <span>Wire PikaORM's PikaLogger to SLF4J, Log4j2, or java.util.logging.</span>
      </div>
      <div class="docs-card-content">
        <ul>
          <li>
            <a href="{{ '/pages/connection-logging/' | relative_url }}">
              <strong>View Logging Pattern</strong>
              <small>Practical examples of integrating standard Java loggers.</small>
            </a>
          </li>
        </ul>
      </div>
    </div>

    <!-- 2. Custom Mapping Pattern -->
    <div class="docs-card-accordion">
      <div class="docs-card-summary">
        <div class="docs-card-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>
        </div>
        <strong>Custom Mapping Pattern</strong>
        <span>Using the static mapping() method to override conventions.</span>
      </div>
      <div class="docs-card-content">
        <ul>
          <li>
            <a href="{{ '/pages/custom-mapping/' | relative_url }}">
              <strong>View Custom Mapping</strong>
              <small>Override table names, columns, and serialize complex types.</small>
            </a>
          </li>
        </ul>
      </div>
    </div>

    <!-- 3. N+1 Avoidance -->
    <div class="docs-card-accordion">
      <div class="docs-card-summary">
        <div class="docs-card-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="10.49"/></svg>
        </div>
        <strong>N+1 Query Avoidance</strong>
        <span>Bulk loading with WHERE IN, query caching, and JOIN-based solutions.</span>
      </div>
      <div class="docs-card-content">
        <ul>
          <li>
            <a href="{{ '/pages/n-plus-1-avoidance/' | relative_url }}">
              <strong>View N+1 Avoidance</strong>
              <small>Patterns and techniques to drastically reduce query volume.</small>
            </a>
          </li>
        </ul>
      </div>
    </div>

    <!-- 4. Query Caching Pattern -->
    <div class="docs-card-accordion">
      <div class="docs-card-summary">
        <div class="docs-card-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="13 2 13 9 20 9"/><path d="M10.3 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h6.5l7.5 7.5V19a2 2 0 0 1-2 2h-5.5"/><circle cx="16" cy="16" r="3"/><path d="m21 21-1.5-1.5"/></svg>
        </div>
        <strong>Query Caching Pattern</strong>
        <span>PikaORM query caching for typical web requests.</span>
      </div>
      <div class="docs-card-content">
        <ul>
          <li>
            <a href="{{ '/pages/query-caching-pattern/' | relative_url }}">
              <strong>View Query Caching</strong>
              <small>Servlet filter pattern, clearing after writes, and edge cases.</small>
            </a>
          </li>
        </ul>
      </div>
    </div>

  </div>

  <h2 style="margin-bottom: 1.5rem; font-size: 1.5rem; border-bottom: 1px solid var(--surface-3); padding-bottom: 0.5rem;">Examples</h2>
  <div class="docs-grid">

    <!-- 1. Connection Pooling Example -->
    <div class="docs-card-accordion">
      <div class="docs-card-summary">
        <div class="docs-card-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14a9 3 0 0 0 18 0V5"/><path d="M3 12a9 3 0 0 0 18 0"/></svg>
        </div>
        <strong>Connection Pooling Example</strong>
        <span>Managing database connection pools efficiently using HikariCP.</span>
      </div>
      <div class="docs-card-content">
        <ul>
          <li>
            <a href="{{ '/pages/patterns/examples/connection-pooling-example/' | relative_url }}">
              <strong>View Connection Pooling Guide</strong>
              <small>Learn how to properly set up and teardown connection pools.</small>
            </a>
          </li>
        </ul>
      </div>
    </div>

    <!-- 2. Web Quickstart -->
    <div class="docs-card-accordion">
      <div class="docs-card-summary">
        <div class="docs-card-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
        </div>
        <strong>Web Quickstart</strong>
        <span>Building a simple web application using PikaORM.</span>
      </div>
      <div class="docs-card-content">
        <ul>
          <li>
            <a href="{{ '/pages/quickstart/' | relative_url }}">
              <strong>View Web Quickstart Guide</strong>
              <small>Step-by-step tutorial on bootstrapping a web backend.</small>
            </a>
          </li>
        </ul>
      </div>
    </div>

  </div>
</div>
